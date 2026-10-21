export const PASSWORD_HINT = "At least 8 characters, with an uppercase letter, a lowercase letter, and a digit.";

export function passwordError(password) {
  if (!password || password.length < 8) return PASSWORD_HINT;
  let upper = false;
  let lower = false;
  let digit = false;
  for (const char of password) {
    if (char >= "A" && char <= "Z") upper = true;
    else if (char >= "a" && char <= "z") lower = true;
    else if (char >= "0" && char <= "9") digit = true;
  }
  return upper && lower && digit ? "" : PASSWORD_HINT;
}
