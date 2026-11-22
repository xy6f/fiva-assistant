"""SSE 线格式化：与 Spring SseEmitter 逐字节兼容。

Spring 输出格式为 `event:<name>\\n` + 一行或多行 `data:<line>\\n` + 空行结尾；
data 中含换行时会拆成多条 data: 行（本前端解析器即按此工作）。
"""


def encode_event(name: str, data: str) -> str:
    """把 (事件名, 数据) 编码为与 Java SseEmitter 相同的 SSE 文本块。"""
    normalized = data.replace("\r\n", "\n").replace("\r", "\n")
    payload = "".join(f"data:{line}\n" for line in normalized.split("\n"))
    return f"event:{name}\n{payload}\n"
