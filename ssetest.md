# 建立 SSE 连接
curl -N -H "Accept: text/event-stream" http://127.0.0.1:8000/sse

# 在另一个终端发送 initialize 请求
curl -X POST -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"elicitation":{}},"clientInfo":{"name":"mcp-client","version":"1.0.0"}}}' \
"http://127.0.0.1:8000/messages/?session_id=YOUR_SESSION_ID"

验证：
curl -X POST -H "Content-Type: application/json" -d "@C:\workspace\bbossgroups\bboss-ai\data.json" "http://127.0.0.1:8000/messages/?session_id=674c689bac56492facefb1cdf82a37f6"
