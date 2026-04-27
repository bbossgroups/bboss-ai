从以下代码发现FluxCreate通过onDispose在注册dispose回调函数时，在current.disposable不为空的情况下会直接执行回调函数，这样会导致无法注册多个回调函数了：

public final FluxSink<T> onDispose(Disposable d) {
			Objects.requireNonNull(d, "onDispose");
			SinkDisposable sd = new SinkDisposable(d, null);
			if (!DISPOSABLE.compareAndSet(this, null, sd)) {
				Disposable c = disposable;
				if (c == TERMINATED || c == CANCELLED) {
					d.dispose();
				}
				else if (c instanceof SinkDisposable) {
					SinkDisposable current = (SinkDisposable) c;
					if (current.disposable == null) {
						current.disposable = d;
					}
					else {
						d.dispose();
					}
				}
			}
			return this;
		}



你的观察非常敏锐，这确实是 `FluxCreate`（以及 `MonoCreate`）中的一个**已知且已文档化的行为**，而非实现上的疏忽。

## 问题确认

你指出的这段代码逻辑：

```java
if (current.disposable == null) {
    current.disposable = d;
} else {
    d.dispose();  // 直接执行回调，导致无法注册多个
}
```

意味着 `SinkDisposable` 最多只能容纳**一个** `onDispose` 回调和一个 `onCancel` 回调。当第二个 `onDispose` 被注册时，由于 `current.disposable != null`，新传入的 `Disposable` 会被立即调用 `dispose()`，而不是被暂存起来等待真正的终止信号。

## 这是已知 Issue

这个行为早在 2020 年就被报告为 GitHub Issue [#2015](https://github.com/reactor/reactor-core/issues/2015) ，报告者给出的复现代例几乎与你描述的场景一致：

```java
Flux.create(sink -> {
  sink.onDispose(() -> cleanedUp.set(true));
  sink.onDispose(() -> cleanedUp.set(true));  // 不应在 sink dispose 前被调用
  assertFalse(cleanedUp.get()); // 这里会失败，因为第二个回调已被立即执行
})
```

当时提出的建议包括：支持多个 Disposable、已注册时抛异常、或在文档中说明。官方最终选择了**在文档中明确约束**的方案。

## 官方文档的明确说明

在最新的 `FluxSink` API 文档中，`onDispose` 和 `onCancel` 的说明都已明确指出 ：

> **onDispose**: "At most one callback can be registered, and subsequent calls to this method will result in the immediate disposal of the extraneous Disposable."
>
> **onCancel**: "At most one callback can be registered, and subsequent calls to this method will result in the immediate disposal of the extraneous Disposable."

也就是说，**"最多只能注册一个回调，重复注册会导致多余的 Disposable 被立即 dispose"** 这是当前的设计契约，而非 Bug。

## 为什么这样设计？

`FluxCreate` 和 `MonoCreate` 内部使用同一个 `SinkDisposable` 对象来同时管理 `onDispose` 和 `onCancel` 两个槽位：

```java
// SinkDisposable 结构示意
class SinkDisposable implements Disposable {
    Disposable disposable;  // onDispose 槽位
    Disposable onCancel;    // onCancel 槽位
}
```

这种设计避免了为每个 `FluxSink` 引入更复杂的集合结构（如 `CompositeDisposable`），在绝大多数"一个 dispose 回调做全部清理"的场景下足够使用，同时保持了轻量。

## 如果需要注册多个回调怎么办？

如果你确实需要多个清理动作，应该在调用 `onDispose` **之前**自行组合成一个 `Disposable`。例如使用 `Disposable.composite`（或 Reactor 的 `Disposables.composite()`）：

```java
Disposable.Composite composite = Disposables.composite();

composite.add(() -> { /* 清理资源 A */ });
composite.add(() -> { /* 清理资源 B */ });
composite.add(() -> { /* 清理资源 C */ });

sink.onDispose(composite);
```

这样 `SinkDisposable` 只持有 `composite` 这一个引用，而当 sink 真正被 dispose 时，`composite.dispose()` 会依次调用你注册的所有清理逻辑。

---

**总结**：你看到的代码行为是符合当前设计的——`onDispose` 和 `onCancel` 各自最多只支持一个回调，重复注册会触发立即 dispose。如果需要多回调，请在外部自行聚合后再注册。