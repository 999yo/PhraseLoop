package com.phraseloop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "メールアドレスを入力してください")
        @Email(message = "メールアドレスの形式が正しくありません")
        @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
        String email,
        @NotBlank(message = "パスワードを入力してください")
        @Size(max = 72, message = "パスワードは72文字以内で入力してください")
        String password
) {
}
