package com.projeto.util;

import java.util.Locale;

public final class Emails {

	private Emails() {
	}

	public static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
