package com.arachneplus;

final class EssenceCalc {
	private EssenceCalc() { }

	static final double PER_KILL = 37.5;

	private static volatile double price = -1;
	private static volatile long fetchedMs = 0;
	private static volatile boolean fetching = false;

	private static final long REFRESH_MS = 120_000L;

	static double price() {
		long now = System.currentTimeMillis();
		if (!fetching && now - fetchedMs > REFRESH_MS && HypixelApi.available()) {
			fetching = true;
			fetchedMs = now;
			Thread t = new Thread(() -> {
				try {
					double p = RankBoard.essencePrice();
					if (p > 0) {
						price = p;
					}
				} finally {
					fetching = false;
				}
			}, "ArachnePlus-essenceprice");
			t.setDaemon(true);
			t.start();
		}
		return price;
	}

	static long coinsPerKill() {
		double p = price();
		return p > 0 ? Math.round(PER_KILL * p) : 0L;
	}
}
