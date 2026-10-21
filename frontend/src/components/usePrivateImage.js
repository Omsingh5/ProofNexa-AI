import { useEffect, useState } from "react";
import { getToken } from "../api.js";

export default function usePrivateImage(path, enabled) {
  const [src, setSrc] = useState("");
  useEffect(() => {
    if (!enabled) {
      setSrc("");
      return undefined;
    }
    let url = "";
    let cancel = false;
    fetch(path, { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((res) => (res.ok ? res.blob() : null))
      .then((blob) => {
        if (!blob || cancel) return;
        url = URL.createObjectURL(blob);
        setSrc(url);
      })
      .catch(() => setSrc(""));
    return () => {
      cancel = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [path, enabled]);
  return src;
}

export async function downloadAuthed(path, filename) {
  const res = await fetch(path, { headers: { Authorization: `Bearer ${getToken()}` } });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || "Could not download the file.");
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename || "download";
  link.click();
  URL.revokeObjectURL(url);
}
