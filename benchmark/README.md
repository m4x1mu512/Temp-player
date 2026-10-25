# Macrobenchmark

Измеряет запуск и кадры приложения на реальном устройстве и генерирует baseline-профиль.
Цель — `benchmark`-сборка приложения: R8 включён, как в релизе, но подпись отладочная,
поэтому keystore не нужен.

## Требования

- Устройство (или эмулятор с API 26+) с включённой USB-отладкой, `adb devices` его видит.
- Замеры делаются только на release-like сборке, поэтому цифры сопоставимы с тем, что
  получает пользователь.

## Замеры

```bash
# холодный и тёплый запуск, 10 итераций
./gradlew :benchmark:connectedDebugAndroidTest
```

Отчёт: `benchmark/build/reports/androidTests/connected/` — медиана, p50/p90/p95 по
`timeToInitialDisplay` и `timeToFullDisplay`.

## Baseline-профиль для Android 8–11

```bash
./gradlew :benchmark:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.benchmark.BaselineProfileGenerator
```

Результат макробенчмарк-библиотека складывает в
`benchmark/build/outputs/managed_device_android_test_additional_output/` (файл
`baseline-prof.txt`). Скопируйте его в `app/src/main/baseline-prof.txt` и пересоберите
release: AGP сольёт его с профилями AndroidX, а `ProfileInstaller` установит на
Android 8–11.

Профиль нужно перегенерировать после крупных изменений в навигации или стартовом экране.
