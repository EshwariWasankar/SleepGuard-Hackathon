from dotenv import load_dotenv
import google.generativeai as genai
import os

load_dotenv()
print("Testing key:", os.getenv("GEMINI_API_KEY")[:20] + "...")

try:
    genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
    model = genai.GenerativeModel('gemini-flash-latest')
    response = model.generate_content("API test")
    print("✅ NEW KEY WORKS:", response.text[:50])
except Exception as e:
    print("❌ KEY ERROR:", str(e))
