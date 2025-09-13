# ParkMate

### 🔹 If you’re using **Android Studio GUI**

1. **Commit** your changes:

   * `Git > Commit` (or the **Commit button** at the top-right).
2. **Push** your changes:

   * `Git > Push` (or the **Push button** on the toolbar).
3. As long as you’re on `main`, Android Studio will push to `main` on GitHub.

---

### 🔹 If you’re using **Terminal / Command Line**

Check you’re on `main`:

```bash
git branch
```

(if `main` has a `*` next to it, you’re good).

Then:

```bash
git add .
git commit -m "Your message"
git push origin main
```

---

### 🔹 First-time setup (only once per repo)

If you want Git to remember and always push to `main` without typing it every time:

```bash
git push -u origin main
```

After that, you can just run:

```bash
git push
```



Do you want me to give you a **one-time setup command** that will force Android Studio to always use `main` automatically?

