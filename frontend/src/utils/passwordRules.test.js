import { describe, expect, it } from "vitest";
import { passwordError } from "./passwordRules.js";

describe("password rules", () => {
  it("accepts a mixed password", () => {
    expect(passwordError("Password123!")).toBe("");
  });

  it("rejects a lowercase-only password", () => {
    expect(passwordError("password")).not.toBe("");
  });
});
