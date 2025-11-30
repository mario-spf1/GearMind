package com.gearmind.tools;

import com.gearmind.infrastructure.auth.BCryptPasswordHasher;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher();

        String raw = "admin";
        String hash = hasher.hash(raw);

        System.out.println("Hash para '" + raw + "':");
        System.out.println(hash);
    }
}
