package dev.x341.mrw.mod;

/**
 * Server-side debug toggle for MRW, controlled by {@code /mrw debug on}/{@code /mrw debug off}
 * (see {@link Init#init()}). When enabled, diagnostic details (wall-side resolution, Rail Worker
 * build parameters, ...) are written to {@link Init#LOGGER} instead of staying silent.
 */
public final class MrwDebug {

	private static volatile boolean enabled;

	private MrwDebug() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}
}
