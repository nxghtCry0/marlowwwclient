import json

transcript_path = r"C:\Users\teeja\.gemini\antigravity\brain\b207dd65-7853-4839-95de-97c04a1fd80b\.system_generated\logs\transcript_full.jsonl"

with open(transcript_path, 'r', encoding='utf-8') as f:
    for idx, line in enumerate(f):
        if not line.strip():
            continue
        try:
            step = json.loads(line)
        except Exception:
            continue
        content = step.get('content', '')
        if content and ('mocha' in content.lower() or 'brown' in content.lower()):
            print(f"Step {idx}: source={step.get('source')}, content={content[:200]}")
