package com.arachneplus;

final class CommandList {
	private CommandList() { }

	static final String[] CATS = {"Stats", "Interactive", "Fun", "Setup"};

	static final String[][] ALL = {
		{"/apstats <username>", "Look up another player's Arachne and Brood kills", "0"},
		{"/apslayer <username>", "Their Spider Slayer level and boss kills", "0"},
		{"/apcompare <username>", "Compare your kills with theirs", "0"},
		{"/apgear <username>", "Their Gyrokinetic Wand, Sting, Kuudra armor and combat level", "0"},
		{"/apparty", "Ranks everyone in your party by Arachne kills", "0"},
		{"/apgoal <kills>", "How long until you reach a kill target, at your pace", "0"},

		{"/aprank on | off", "Join or leave the global leaderboard (/aprank shows your place)", "1"},
		{"/aptop [page]", "Shows the global leaderboard, 10 players per page", "1"},
		{"/apweb  or  /apwebsite", "Clickable links to the website, your player page, guides", "1"},
		{"/appf [crystals] [note]", "Copies a party advert and posts it to Discord (T1-T5, placer, gyro ping)", "1"},
		{"/apmsg", "Says \"Stand by me for quicker bosses\" in chat", "1"},

		{"/apkill <name> <mob>", "Makes a fake death message only you see, and copies it for party chat", "2"},

		{"/ap  or  /a+", "Open the main menu", "3"},
		{"/apgui  or  /a+gui", "Open the config screen", "3"},
		{"/aplist", "Show this list", "3"},
		{"/apupdate [on|off]", "Check for a new version now, or switch the alerts on or off", "3"},
		{"/apkeyhelp", "Prints a search to copy into Google for getting an API key", "3"},
		{"/apapikey <key>", "Optional Hypixel API key: faster kill updates (no key = remove it)", "3"},
		{"/apbestiary <arachne> <brood>", "Set your Arachne and Brood kills by hand", "3"},
		{"/apreset", "Reset the session (profit + kill timer)", "3"},
		{"/apkeygen", "Backup: prints a key that holds your achievement progress (click to copy)", "3"},
		{"/apredeem <code>", "Use a one-time achievement code you were given by the Arachne Plus team", "3"},
		{"/apkey <key>", "Restore your achievement progress from a backup key after an update", "3"},
	};

	static void sayAll() {
		ArachnePlusClient.say("Commands (you can also start any of them with a+ instead of ap):");
		for (int cat = 0; cat < CATS.length; cat++) {
			ArachnePlusClient.say("-- " + CATS[cat] + " --");
			for (String[] c : ALL) {
				if (Integer.parseInt(c[2]) == cat) {
					ArachnePlusClient.say(c[0] + "  -  " + c[1]);
				}
			}
		}
	}
}
