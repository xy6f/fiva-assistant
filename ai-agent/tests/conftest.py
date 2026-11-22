"""让 pytest 能导入 app 包（tests 无 __init__.py，需手动加入项目根目录）。"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
