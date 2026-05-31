import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { JsonBlock, parseJsonText, stringifyJson } from "./JsonBlock";

describe("JsonBlock", () => {
  it("renders formatted JSON detail", () => {
    render(<JsonBlock title="运行输出" value={{ status: "SUCCESS" }} />);

    expect(screen.getByText("运行输出")).toBeInTheDocument();
    expect(screen.getByText(/SUCCESS/)).toBeInTheDocument();
  });

  it("parses and formats JSON text consistently", () => {
    const value = parseJsonText("{\"enabled\":true}");

    expect(stringifyJson(value)).toContain("\"enabled\": true");
  });
});
