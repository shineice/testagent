# 留我自己看 (keepforme)

Android 版的「留我自己看」:在 Threads、IG 或任何 App 按分享,選「留我自己看」,
App 會把分享的內容存下來,並在背景抓取連結指向的整篇貼文(全文、圖片、影片)。

## 使用
1. 用 Android Studio 開啟本專案,執行 `keepforme` 模組(minSdk 24)。
2. 在 Threads / IG 按分享 → 選「留我自己看」。
3. 開啟 App 查看、搜尋收藏。

## 運作方式
- `ShareReceiverActivity`:分享選單的接收端。文字、單張或多張圖片/影片都收;分享進來的媒體會複製到 App 私有資料夾。
- `SaveService`:若分享內容含網址,背景抓取該頁的 Open Graph 資訊(標題、內文、圖片、影片)並下載。
- `EntryStore`:每篇一個資料夾 `filesDir/entries/<id>/`(`entry.json` 加媒體檔),全部只存在手機本機。

## 限制
- 抓取只能拿到網頁公開的 Open Graph 資訊:多張圖的輪播通常只有第一張,長文可能被截斷,需登入才看得到的貼文抓不到。
  抓不到時會保留分享時的原始文字,狀態顯示「沒抓到全文」。
- 想完整保留輪播圖片:在 App 內長按圖片分享/儲存到相簿,再用分享圖片的方式分享給本 App。
- 影片只下載 `og:video` 提供的那一支,超過 60MB 略過。
