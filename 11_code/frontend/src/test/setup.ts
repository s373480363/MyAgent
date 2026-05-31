import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();
});

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false
  })
});

class ResizeObserverMock {
  /**
   * 开始监听尺寸变化。
   */
  observe() {
    return undefined;
  }

  /**
   * 停止监听目标。
   */
  unobserve() {
    return undefined;
  }

  /**
   * 断开监听。
   */
  disconnect() {
    return undefined;
  }
}

Object.defineProperty(window, "ResizeObserver", {
  writable: true,
  value: ResizeObserverMock
});
