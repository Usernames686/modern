import axios from "axios";

export const frontApiBase = (import.meta.env.VITE_FRONT_API_BASE || "/api/front").replace(/\/+$/, "");
export const frontAssetOrigin = resolveAssetOrigin(frontApiBase);

export function createFrontClient(getToken) {
  function authHeaders() {
    const token = getToken?.();
    return token ? { "Authori-zation": token } : {};
  }

  async function get(url, params, auth = false) {
    const response = await axios.get(apiUrl(url), { params, headers: auth ? authHeaders() : {} });
    if (response.data.code !== 200) {
      throw new Error(response.data.message || "接口请求失败");
    }
    return response.data.data;
  }

  async function post(url, data, auth = false) {
    const response = await axios.post(apiUrl(url), data, { headers: auth ? authHeaders() : {} });
    if (response.data.code !== 200) {
      throw new Error(response.data.message || "接口请求失败");
    }
    return response.data.data;
  }

  async function upload(url, formData, auth = false) {
    const response = await axios.post(apiUrl(url), formData, {
      headers: { ...(auth ? authHeaders() : {}), "Content-Type": "multipart/form-data" }
    });
    if (response.data.code !== 200) {
      throw new Error(response.data.message || "上传失败");
    }
    return response.data.data;
  }

  return { get, post, upload };
}

export function apiUrl(url) {
  if (frontApiBase === "/api/front" || !url.startsWith("/api/front")) {
    return url;
  }
  return frontApiBase + url.slice("/api/front".length);
}

export function assetUrl(value) {
  if (!value) {
    return "";
  }
  if (/^https?:\/\//.test(value)) {
    try {
      const parsed = new URL(value);
      if (parsed.pathname.startsWith("/crmebimage") || parsed.pathname.startsWith("/public")) {
        const assetOrigin = frontAssetOrigin || (typeof window !== "undefined" ? window.location.origin : "");
        return `${assetOrigin}${parsed.pathname}${parsed.search}${parsed.hash}`;
      }
    } catch {
      return value;
    }
    return value;
  }
  const normalized = value.startsWith("/") ? value : `/${value}`;
  if (frontAssetOrigin && (normalized.startsWith("/crmebimage") || normalized.startsWith("/public"))) {
    return frontAssetOrigin + normalized;
  }
  return normalized;
}

function resolveAssetOrigin(base) {
  try {
    return new URL(base).origin;
  } catch {
    return "";
  }
}
