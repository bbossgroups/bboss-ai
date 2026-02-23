chatobject设置message.getGenAudioCompletionsUrl()地址

tool报文：

{"id":"93d791d7-8962-4ea0-96e3-f43d6025d7ee","object":"chat.completion","created":1770644807,"model":"deepseek-chat","choices":[{"index":0,"message":{"role":"assistant","content":"我来帮您查询杭州的天气情况，然后根据天气为您提供相应的建议。","tool_calls":[{"index":0,"id":"call_00_wDHWkQUNocjLGxzPDdtjIFUt","type":"function","function":{"name":"get_weather","arguments":"{\"location\": \"杭州\"}"}}]},"logprobs":null,"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":329,"completion_tokens":60,"total_tokens":389,"prompt_tokens_details":{"cached_tokens":0},"prompt_cache_hit_tokens":0,"prompt_cache_miss_tokens":329},"system_fingerprint":"fp_eaab8d114b_prod0820_fp8_kvcache"}