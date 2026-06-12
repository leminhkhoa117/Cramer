"""Visual verification screenshots for the Cramer frontend rewrite."""
import sys
from playwright.sync_api import sync_playwright

PAGES = [
    ("home", "http://localhost:3000/"),
]
OUT = r"e:\IT and Computer Knowledges\Cramer\frontend\.verify"

import os
os.makedirs(OUT, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1366, "height": 900})
    for name, url in PAGES:
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            page.wait_for_timeout(500)
            # Scroll through the page to trigger framer-motion whileInView animations
            height = page.evaluate("document.body.scrollHeight")
            step = 700
            y = 0
            while y < height:
                page.evaluate(f"window.scrollTo(0, {y})")
                page.wait_for_timeout(180)
                y += step
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            page.wait_for_timeout(400)
            page.evaluate("window.scrollTo(0, 0)")
            page.wait_for_timeout(300)
            page.screenshot(path=f"{OUT}/{name}.png", full_page=True)
            print(f"OK {name}: {url}")
            # capture console errors
        except Exception as e:
            print(f"FAIL {name}: {e}")
    browser.close()
print("done")
