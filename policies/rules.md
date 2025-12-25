# Agent Decision Policies + Scientific Thresholds

## Caffeine (Half-life ~5hrs [web:1])
- **High risk**: >50mg remaining 4hrs before sleep
- **Medium**: 20-50mg remaining 6hrs before sleep
- **Intervene**: Intake <6hrs before target sleep

## Melatonin Suppression [web:2]
>1hr screen last 2hrs → 0.4 risk
>100min + brightness>0.8 → 0.8 risk (blue light peak 460-480nm)

## Noise Disruption [web:3]
<45dB → low risk (sleep optimal)
45-65dB → 0.3 risk (micro-arousals)
>65dB → 0.7+ risk (REM disruption)

## Sleep Debt
>3hrs → HIGH (cognitive impairment)
1.5-3hrs → MEDIUM
<1.5hrs → LOW

## Escalation Rules
1. **Multiple MEDIUM** → URGENCY: HIGH
2. **Any HIGH** → URGENCY: HIGH + strongest intervention
3. **Time pressure** (<2hrs to sleep) → ADJUST_ALARM preferred
