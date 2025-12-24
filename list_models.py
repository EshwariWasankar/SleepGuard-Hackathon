from dotenv import load_dotenv
import os
import google.generativeai as genai

load_dotenv()

genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

print("AVAILABLE MODELS:\n")

for model in genai.list_models():
    print(model.name)
