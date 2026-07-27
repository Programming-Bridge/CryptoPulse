# Implementation Plan - Portfolio Growth Tracking

Implement a dynamic snapshot system to track and visualize the user's Total Net Worth growth over time.

## User Review Required

> [!IMPORTANT]
> - **Initial Backfill**: Since you are just starting, the app will generate a simulated 7-day history based on your current balance and transaction dates so you can see a chart immediately.
> - **Daily Logic**: Going forward, the app will save your exact total balance once per day to build a 100% accurate lifetime growth chart.

## Proposed Changes

### Logic & Data

#### [MODIFY] [CryptoViewModel.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/viewmodels/CryptoViewModel.kt)
- **Snapshot Logic**: Add a function `saveDailySnapshot()` called after every successful sync.
- **Backfill Logic**: Create a function to generate historical snapshots for the first 7 days if the database is empty.
- **Expose History**: Expose `portfolioHistory` StateFlow (converted from Repository Flow).

### UI Enhancements

#### [MODIFY] [AnalyticsScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/AnalyticsScreen.kt)
- **Real Performance Chart**: Replace `PerformanceBarChart` (static) with `PortfolioGrowthChart` (dynamic).
- Use the actual `SnapshotEntity` data to draw the bars/lines.
- Update labels to show "7-Day Growth" based on the data.

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/screens/DashboardScreen.kt)
- **Dynamic Sparkline**: Update `PortfolioCard` to use your **actual portfolio history** for the sparkline chart instead of the BTC placeholder.

#### [MODIFY] [Components.kt](file:///C:/Users/hamza/AndroidStudioProjects/CryptoPulse/app/src/main/java/com/cryptopulse/ui/components/Components.kt)
- Update `SparklineChart` to handle empty states and varying data ranges more robustly.

## Verification Plan

### Manual Verification
1.  **Dashboard Chart**: Open the app. Verify the small sparkline in the "Total Balance" card reflects your portfolio (it should show a trend based on your transaction history).
2.  **Analytics Detail**: Go to the Analytics tab. Verify the "Monthly Performance" chart now has real bars corresponding to your wealth snapshots.
3.  **Data Persistence**: Close and re-open the app. Verify the chart data persists.
