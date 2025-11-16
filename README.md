# Timesignal

Pixel Watch 向けの時報アプリです。毎時 0 / 15 / 30 / 45 分の 4 つのタイミングに対して、ON/OFF の切り替えと複数のバイブ時間プリセットからの選択ができます。設定画面で ON に切り替える、もしくはバイブ時間を選択すると、その場で指定した長さのバイブレーションが再生されてフィードバックします。

さらに、お休み時間を設定している間はバイブが動作しません。開始・終了時刻は 24 時間表記で設定でき、深夜をまたぐケースにも対応しています。

## 主な構成

- `TimesignalViewModel` – DataStore に保存された設定を購読し UI に状態を提供。切り替えやプリセット変更時に `TimesignalScheduler` を呼び出して AlarmManager を再スケジュールし、同時にバイブレーションも発生させます。
- `TimesignalScheduler` – 4 つの四半期スロットごとに次の発火時刻を計算し、`QuarterChimeReceiver` を Exact alarm として登録します。
- `QuarterChimeReceiver` – アラーム発火時に設定とお休み時間を確認し、許可されている場合のみ `TimesignalVibrator` を通してバイブを動作させます。発火後は次の 15 分先を再度予約します。
- Compose for Wear OS UI (`TimesignalScreen`) – 各スロットのカードとお休み時間カードで構成され、スイッチやプリセット Chip、TimePicker ダイアログで設定を変更できます。

## ビルド

通常の Android Studio で Wear OS モジュールとして開いてビルドできます。

## Android Studio の Run/Debug 設定

1. **プロジェクトを開く** – 「Open」からこのリポジトリのルート (`settings.gradle.kts` がある階層) を指定すると、`app` モジュールが Wear OS アプリとして読み込まれます。
2. **デバイスを接続** – Pixel Watch を Wi-Fi ADB で接続するか、Wear OS エミュレーターを起動しておきます。Android Studio 右上のデバイスセレクターに表示されることを確認します。
3. **構成を作成** – ツールバーの実行構成ドロップダウンから `Edit Configurations...` を開き、「Wear OS App」テンプレートを新規作成します。`Module` は `app` を選択し、`Launch Options` で「Default Activity」または `MainActivity` を指定します。
4. **デプロイターゲットを指定** – 作成した構成の `Deployment Target Options` で「USB/Wi-Fi Connected Device」を選び、接続済みの Pixel Watch もしくはエミュレーターをターゲットに設定します。
5. **Run / Debug** – ツールバーで作成した構成を選択し、▶ ボタンで通常実行、または虫アイコンでデバッグを開始します。初回起動時には必要な依存関係のダウンロードとビルドが行われ、そのままウォッチ上でアプリが自動的に立ち上がります。

設定変更や Quiet hours の検証などを行う際は、`QuarterChimeReceiver` に対して `adb shell am broadcast` コマンドを送ると即座に挙動が確認できるため、Run/Debug 構成を維持したまま素早く反復できます。
