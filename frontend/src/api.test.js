import { describe, expect, it } from "vitest";
import { clearSession, getToken, setSession } from "./api.js";

describe("session helpers", () => {
  it("stores and clears the JWT", () => {
    setSession("abc", { email: "a@b.c" });
    expect(getToken()).toBe("abc");
    clearSession();
    expect(getToken()).toBeNull();
  });
});
