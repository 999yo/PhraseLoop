package com.phraseloop.dto;

import com.phraseloop.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatch
public record RegisterRequest(
        @NotBlank(message = "メールアドレスを入力してください")
        @Email(message = "メールアドレスの形式が正しくありません")
        @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
        String email,
        @NotBlank(message = "パスワードを入力してください")
        @Size(min = 8, max = 72, message = "パスワードは8文字以上72文字以内で入力してください")
        String password,
        @NotBlank(message = "確認用パスワードを入力してください")
        String passwordConfirm
) {
}
