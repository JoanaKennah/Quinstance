# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: lobby.spec.js >> Client can join lobby with valid room code and appears in player list
- Location: src\test\playwright-tests\lobby.spec.js:34:5

# Error details

```
Test timeout of 30000ms exceeded.
```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - generic [ref=e3]: COMP2870 Quiz
  - generic [ref=e4]:
    - 'heading "Room Code: UR0TQYUC" [level=1] [ref=e6]'
    - generic [ref=e9]:
      - button "Start Quiz" [ref=e11] [cursor=pointer]
      - button "Settings" [ref=e13] [cursor=pointer]
      - button "Close Lobby" [ref=e15] [cursor=pointer]
```