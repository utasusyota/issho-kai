# APK生成

このプロジェクトはGitHub ActionsでAPKを生成できます。

1. GitHubの新規リポジトリにこのフォルダ一式を置く
2. mainブランチへpush
3. Actions > Build installable APK を開く
4. 完了後 Artifacts > issho-kai-v1.0-apk を取得
5. zipを展開して `issho-kai-v1.0.apk` をAndroid端末へコピー
6. Androidで「不明なアプリのインストール」を一時的に許可してAPKを開く

Debug APKなのでGoogle Play公開用ではありませんが、自分のF-52Gなどへ直接インストールして試せます。
