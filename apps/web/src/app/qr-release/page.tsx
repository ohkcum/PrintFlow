"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import {
  QrCode,
  Camera,
  Search,
  FileText,
  CheckCircle,
  AlertCircle,
  Loader2,
  RefreshCw,
  Monitor,
} from "lucide-react";
import { qrApi } from "@/lib/api";

// ─── Shared Components ─────────────────────────────────────────────────────────

function btnStyle(
  variant: "primary" | "secondary" | "danger" | "ghost",
): React.CSSProperties {
  const base: React.CSSProperties = {
    padding: "0.5rem 1rem",
    borderRadius: "var(--radius-md)",
    fontSize: "0.85rem",
    fontWeight: 600,
    cursor: "pointer",
    border: "none",
    transition: "opacity 0.15s",
    display: "inline-flex",
    alignItems: "center",
    gap: "0.375rem",
  };
  if (variant === "primary")
    return {
      ...base,
      background: "var(--color-primary)",
      color: "var(--color-primary-foreground)",
    };
  if (variant === "danger")
    return { ...base, background: "oklch(0.65 0.22 25)", color: "white" };
  if (variant === "ghost")
    return {
      ...base,
      background: "transparent",
      color: "var(--color-muted-foreground)",
      border: "1px solid var(--color-border)",
    };
  return {
    ...base,
    background: "var(--color-secondary)",
    color: "var(--color-secondary-foreground)",
    border: "1px solid var(--color-border)",
  };
}

function Toast({
  message,
  type,
}: {
  message: string;
  type: "success" | "error" | "info";
}) {
  return (
    <div
      style={{
        position: "fixed",
        bottom: "1.5rem",
        right: "1.5rem",
        zIndex: 200,
        padding: "0.75rem 1.25rem",
        borderRadius: "var(--radius-md)",
        background:
          type === "success"
            ? "oklch(0.72 0.18 145)"
            : type === "error"
              ? "oklch(0.65 0.22 25)"
              : "var(--color-primary)",
        color: "white",
        fontSize: "0.875rem",
        fontWeight: 600,
        boxShadow: "var(--shadow-lg)",
        animation: "fadeIn 0.2s ease",
        maxWidth: "350px",
      }}
    >
      {message}
    </div>
  );
}

// ─── QR Scanner Component ───────────────────────────────────────────────────────

function QRScanner({
  onResult,
  onError,
}: {
  onResult: (result: string) => void;
  onError: (error: string) => void;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [scanning, setScanning] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [availableCameras, setAvailableCameras] = useState<MediaDeviceInfo[]>([]);
  const [selectedCamera, setSelectedCamera] = useState<string>("");
  const streamRef = useRef<MediaStream | null>(null);
  const scanIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
      scanIntervalRef.current = null;
    }
    setScanning(false);
  }, []);

  const startCamera = useCallback(async () => {
    setCameraError(null);
    stopCamera();

    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const cameras = devices.filter((d) => d.kind === "videoinput");
      setAvailableCameras(cameras);

      if (cameras.length === 0) {
        setCameraError("No cameras found on this device");
        return;
      }

      const constraints: MediaStreamConstraints = {
        video: {
          facingMode: "environment",
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      };

      if (selectedCamera) {
        (constraints.video as MediaTrackConstraints).deviceId = {
          exact: selectedCamera,
        };
      }

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setScanning(true);

        // Start scanning frames
        scanIntervalRef.current = setInterval(async () => {
          await scanFrame();
        }, 500);
      }
    } catch (err: any) {
      if (err.name === "NotAllowedError") {
        setCameraError("Camera access denied. Please allow camera permissions.");
      } else if (err.name === "NotFoundError") {
        setCameraError("No camera found on this device.");
      } else {
        setCameraError("Could not start camera: " + err.message);
      }
      onError(cameraError ?? "Camera error");
    }
  }, [selectedCamera, stopCamera, onError, cameraError]);

  const scanFrame = useCallback(async () => {
    if (!videoRef.current || !canvasRef.current || !scanning) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    if (video.readyState !== video.HAVE_ENOUGH_DATA) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0);

    try {
      // Dynamic import of @zxing/browser
      const { BrowserQRCodeReader } = await import("@zxing/browser");
      const reader = new BrowserQRCodeReader();

      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
      const luminanceSource = {
        getWidth: () => canvas.width,
        getHeight: () => canvas.height,
        getMatrix: () => {
          const data = new Uint8Array(canvas.width * canvas.height);
          for (let i = 0; i < imageData.data.length; i += 4) {
            data[i / 4] = (imageData.data[i] * 0.299 +
              imageData.data[i + 1] * 0.587 +
              imageData.data[i + 2] * 0.114);
          }
          return data;
        },
        isInverted: () => false,
      };

      // Try to decode
      const binaryBitmap = {
        getBlackMatrix: () => ({
          getRow: (y: number, row: any) => {
            const data = luminanceSource.getMatrix();
            const width = luminanceSource.getWidth();
            const start = y * width;
            for (let x = 0; x < width; x++) {
              row[x] = data[start + x] < 128 ? 1 : 0;
            }
            return row;
          },
          getHeight: () => luminanceSource.getHeight(),
          getWidth: () => luminanceSource.getWidth(),
        }),
      };

      // Use simpler approach - call ZXing directly on canvas
      const result = await reader.decodeFromCanvas(canvas);
      if (result) {
        stopCamera();
        onResult(result.getText());
      }
    } catch {
      // No QR code found in this frame
    }
  }, [scanning, onResult, stopCamera]);

  useEffect(() => {
    return () => {
      stopCamera();
    };
  }, [stopCamera]);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
      {/* Camera selector */}
      {availableCameras.length > 1 && (
        <div>
          <label
            style={{
              display: "block",
              fontSize: "0.8rem",
              fontWeight: 600,
              marginBottom: "0.375rem",
            }}
          >
            Camera
          </label>
          <select
            value={selectedCamera}
            onChange={(e) => {
              setSelectedCamera(e.target.value);
            }}
            style={{
              width: "100%",
              padding: "0.5rem 0.75rem",
              background: "var(--color-input)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              color: "var(--color-foreground)",
              fontSize: "0.875rem",
              outline: "none",
              cursor: "pointer",
            }}
          >
            {availableCameras.map((cam) => (
              <option key={cam.deviceId} value={cam.deviceId}>
                {cam.label || `Camera ${availableCameras.indexOf(cam) + 1}`}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Video / Scanner */}
      <div
        style={{
          position: "relative",
          width: "100%",
          maxWidth: "400px",
          margin: "0 auto",
          borderRadius: "var(--radius-lg)",
          overflow: "hidden",
          background: "black",
          aspectRatio: "1",
        }}
      >
        <video
          ref={videoRef}
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            display: scanning ? "block" : "none",
          }}
          playsInline
          muted
        />
        <canvas ref={canvasRef} style={{ display: "none" }} />

        {!scanning && !cameraError && (
          <div
            style={{
              position: "absolute",
              inset: 0,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              background: "oklch(0.12 0.02 250)",
              color: "var(--color-muted-foreground)",
              gap: "1rem",
            }}
          >
            <QrCode size={64} style={{ opacity: 0.3 }} />
            <span style={{ fontSize: "0.875rem" }}>Camera preview</span>
          </div>
        )}

        {/* Scanning overlay */}
        {scanning && (
          <div
            style={{
              position: "absolute",
              inset: 0,
              pointerEvents: "none",
            }}
          >
            {/* Corner brackets */}
            {[
              { top: "15%", left: "15%" },
              { top: "15%", right: "15%", transform: "rotate(90deg)" },
              { bottom: "15%", right: "15%", transform: "rotate(180deg)" },
              { bottom: "15%", left: "15%", transform: "rotate(270deg)" },
            ].map((style, i) => (
              <div
                key={i}
                style={{
                  position: "absolute",
                  width: "40px",
                  height: "40px",
                  borderColor: "oklch(0.72 0.18 145)",
                  borderStyle: "solid",
                  borderWidth: "3px",
                  borderRadius: "4px",
                  borderRightWidth: "0",
                  borderBottomWidth: "0",
                  ...style,
                }}
              />
            ))}
            <div
              style={{
                position: "absolute",
                bottom: "1rem",
                left: "50%",
                transform: "translateX(-50%)",
                background: "oklch(0 0 0 / 0.6)",
                color: "white",
                padding: "0.375rem 0.75rem",
                borderRadius: "var(--radius-md)",
                fontSize: "0.75rem",
                display: "flex",
                alignItems: "center",
                gap: "0.375rem",
              }}
            >
              <div
                style={{
                  width: "8px",
                  height: "8px",
                  borderRadius: "50%",
                  background: "oklch(0.72 0.18 145)",
                  animation: "pulse 1s infinite",
                }}
              />
              Scanning...
            </div>
          </div>
        )}
      </div>

      {/* Camera error */}
      {cameraError && (
        <div
          style={{
            padding: "1rem",
            background: "oklch(0.65 0.22 25 / 0.1)",
            border: "1px solid oklch(0.65 0.22 25 / 0.3)",
            borderRadius: "var(--radius-md)",
            color: "oklch(0.65 0.22 25)",
            fontSize: "0.875rem",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
          }}
        >
          <AlertCircle size={16} />
          {cameraError}
        </div>
      )}

      {/* Start/Stop button */}
      <div style={{ display: "flex", gap: "0.75rem", justifyContent: "center" }}>
        {!scanning ? (
          <button onClick={startCamera} style={btnStyle("primary")}>
            <Camera size={16} /> Start Camera
          </button>
        ) : (
          <button onClick={stopCamera} style={btnStyle("danger")}>
            <AlertCircle size={16} /> Stop Camera
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Document Info Display ─────────────────────────────────────────────────────

function DocumentInfo({
  uuid,
  onBack,
  onToast,
}: {
  uuid: string;
  onBack: () => void;
  onToast: (msg: string, type: "success" | "error" | "info") => void;
}) {
  const [loading, setLoading] = useState(true);
  const [releasing, setReleasing] = useState(false);
  const [docInfo, setDocInfo] = useState<{
    documentId: number;
    docName: string;
    pageCount: number;
    status: string;
    expired: boolean;
    message: string;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [released, setReleased] = useState(false);

  const validateDocument = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await qrApi.validate(uuid);
      setDocInfo(result.data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [uuid]);

  useEffect(() => {
    validateDocument();
  }, [validateDocument]);

  async function handleRelease() {
    if (!docInfo || docInfo.expired || docInfo.status !== "PENDING") return;

    setReleasing(true);
    try {
      const result = await qrApi.release(uuid);
      setReleased(true);
      onToast(
        `Document "${result.data.docName}" released successfully!`,
        "success",
      );
    } catch (e: any) {
      onToast(e.message, "error");
    } finally {
      setReleasing(false);
    }
  }

  if (loading) {
    return (
      <div
        style={{
          padding: "3rem",
          textAlign: "center",
          color: "var(--color-muted-foreground)",
        }}
      >
        <Loader2
          size={32}
          style={{
            margin: "0 auto 1rem",
            animation: "spin 1s linear infinite",
          }}
        />
        Validating document...
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          background: "var(--color-card)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-lg)",
        }}
      >
        <div
          style={{
            width: "60px",
            height: "60px",
            borderRadius: "50%",
            background: "oklch(0.65 0.22 25 / 0.12)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            margin: "0 auto 1rem",
          }}
        >
          <AlertCircle
            size={32}
            style={{ color: "oklch(0.65 0.22 25)" }}
          />
        </div>
        <div
          style={{
            fontSize: "1.1rem",
            fontWeight: 700,
            marginBottom: "0.5rem",
          }}
        >
          Invalid QR Code
        </div>
        <div
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.875rem",
            marginBottom: "1.5rem",
          }}
        >
          {error}
        </div>
        <button onClick={onBack} style={btnStyle("secondary")}>
          <RefreshCw size={16} /> Try Again
        </button>
      </div>
    );
  }

  if (!docInfo) return null;

  return (
    <div
      style={{
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-xl)",
        padding: "2rem",
        maxWidth: "450px",
        margin: "0 auto",
        textAlign: "center",
      }}
    >
      {/* Icon */}
      <div
        style={{
          width: "72px",
          height: "72px",
          borderRadius: "50%",
          background: docInfo.expired
            ? "oklch(0.65 0.22 25 / 0.12)"
            : docInfo.status === "RELEASED"
              ? "oklch(0.72 0.18 145 / 0.12)"
              : "oklch(0.72 0.18 250 / 0.12)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          margin: "0 auto 1.25rem",
        }}
      >
        {released || docInfo.status === "RELEASED" ? (
          <CheckCircle
            size={36}
            style={{ color: "oklch(0.72 0.18 145)" }}
          />
        ) : docInfo.expired ? (
          <AlertCircle
            size={36}
            style={{ color: "oklch(0.65 0.22 25)" }}
          />
        ) : (
          <FileText
            size={36}
            style={{ color: "oklch(0.72 0.18 250)" }}
          />
        )}
      </div>

      {/* Title */}
      <div
        style={{
          fontSize: "1.1rem",
          fontWeight: 700,
          marginBottom: "0.5rem",
        }}
      >
        {released || docInfo.status === "RELEASED"
          ? "Document Released!"
          : docInfo.expired
            ? "Document Expired"
            : "Document Found"}
      </div>

      {/* Document Details */}
      <div
        style={{
          background: "var(--color-background)",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-md)",
          padding: "1rem",
          marginBottom: "1.25rem",
          textAlign: "left",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "0.5rem 0",
            borderBottom: "1px solid var(--color-border)",
          }}
        >
          <span
            style={{
              fontSize: "0.8rem",
              color: "var(--color-muted-foreground)",
            }}
          >
            Document
          </span>
          <span
            style={{
              fontSize: "0.875rem",
              fontWeight: 600,
              maxWidth: "200px",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {docInfo.docName}
          </span>
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "0.5rem 0",
            borderBottom: "1px solid var(--color-border)",
          }}
        >
          <span
            style={{
              fontSize: "0.8rem",
              color: "var(--color-muted-foreground)",
            }}
          >
            Pages
          </span>
          <span
            style={{
              fontSize: "0.875rem",
              fontFamily: "var(--font-mono)",
              fontWeight: 600,
            }}
          >
            {docInfo.pageCount}
          </span>
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "0.5rem 0",
          }}
        >
          <span
            style={{
              fontSize: "0.8rem",
              color: "var(--color-muted-foreground)",
            }}
          >
            Status
          </span>
          <span
            style={{
              padding: "2px 10px",
              borderRadius: "999px",
              fontSize: "0.75rem",
              fontWeight: 600,
              background:
                docInfo.status === "RELEASED"
                  ? "oklch(0.72 0.18 145 / 0.12)"
                  : docInfo.expired
                    ? "oklch(0.65 0.22 25 / 0.12)"
                    : "oklch(0.72 0.18 250 / 0.12)",
              color:
                docInfo.status === "RELEASED"
                  ? "oklch(0.72 0.18 145)"
                  : docInfo.expired
                    ? "oklch(0.65 0.22 25)"
                    : "oklch(0.72 0.18 250)",
            }}
          >
            {docInfo.status}
            {docInfo.expired ? " (Expired)" : ""}
          </span>
        </div>
      </div>

      {/* Message */}
      <div
        style={{
          fontSize: "0.875rem",
          color: "var(--color-muted-foreground)",
          marginBottom: "1.5rem",
        }}
      >
        {docInfo.message}
      </div>

      {/* Actions */}
      <div style={{ display: "flex", gap: "0.75rem", justifyContent: "center" }}>
        {released || docInfo.status === "RELEASED" ? (
          <button onClick={onBack} style={btnStyle("primary")}>
            <QrCode size={16} /> Scan Another
          </button>
        ) : !docInfo.expired && docInfo.status === "PENDING" ? (
          <>
            <button onClick={onBack} style={btnStyle("ghost")}>
              Cancel
            </button>
            <button
              onClick={handleRelease}
              disabled={releasing}
              style={{
                ...btnStyle("primary"),
                opacity: releasing ? 0.7 : 1,
              }}
            >
              {releasing ? (
                <>
                  <Loader2
                    size={16}
                    style={{ animation: "spin 1s linear infinite" }}
                  />
                  Releasing...
                </>
              ) : (
                <>
                  <CheckCircle size={16} /> Release Document
                </>
              )}
            </button>
          </>
        ) : (
          <button onClick={onBack} style={btnStyle("secondary")}>
            <RefreshCw size={16} /> Scan Another
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function QRReleasePage() {
  const [mode, setMode] = useState<"input" | "result">("input");
  const [uuid, setUuid] = useState("");
  const [manualInput, setManualInput] = useState("");
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error" | "info";
  } | null>(null);
  const [cameraMode, setCameraMode] = useState(false);

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(t);
    }
  }, [toast]);

  function showToast(msg: string, type: "success" | "error" | "info") {
    setToast({ message: msg, type });
  }

  function handleScan(result: string) {
    setUuid(result);
    setMode("result");
  }

  function handleManualSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!manualInput.trim()) {
      showToast("Please enter a UUID", "error");
      return;
    }
    setUuid(manualInput.trim());
    setMode("result");
  }

  function handleBack() {
    setMode("input");
    setUuid("");
    setManualInput("");
    setCameraMode(false);
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "var(--color-background)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "2rem",
      }}
    >
      {/* Header */}
      <div
        style={{
          textAlign: "center",
          marginBottom: "2rem",
        }}
      >
        <div
          style={{
            width: "64px",
            height: "64px",
            borderRadius: "16px",
            background: "linear-gradient(135deg, oklch(0.72 0.18 250), oklch(0.55 0.22 280))",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            margin: "0 auto 1rem",
          }}
        >
          <Monitor size={28} color="white" />
        </div>
        <h1
          style={{
            fontSize: "1.75rem",
            fontWeight: 700,
            letterSpacing: "-0.02em",
            marginBottom: "0.5rem",
          }}
        >
          Scan &amp; Print
        </h1>
        <p
          style={{
            color: "var(--color-muted-foreground)",
            fontSize: "0.9rem",
            maxWidth: "360px",
            margin: "0 auto",
          }}
        >
          Scan a QR code or enter the release code to print your document
        </p>
      </div>

      {/* Content */}
      <div style={{ width: "100%", maxWidth: "500px" }}>
        {mode === "result" && uuid ? (
          <DocumentInfo
            uuid={uuid}
            onBack={handleBack}
            onToast={showToast}
          />
        ) : (
          <div
            style={{
              background: "var(--color-card)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-xl)",
              padding: "2rem",
            }}
          >
            {/* Mode toggle */}
            <div
              style={{
                display: "flex",
                gap: "2px",
                background: "var(--color-background)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                padding: "2px",
                marginBottom: "1.5rem",
              }}
            >
              <button
                onClick={() => setCameraMode(false)}
                style={{
                  flex: 1,
                  padding: "0.5rem",
                  borderRadius: "calc(var(--radius-md) - 2px)",
                  border: "none",
                  background: !cameraMode
                    ? "var(--color-card)"
                    : "transparent",
                  color: !cameraMode
                    ? "var(--color-foreground)"
                    : "var(--color-muted-foreground)",
                  fontSize: "0.85rem",
                  fontWeight: !cameraMode ? 600 : 400,
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "0.5rem",
                }}
              >
                <Search size={16} />
                Manual Input
              </button>
              <button
                onClick={() => setCameraMode(true)}
                style={{
                  flex: 1,
                  padding: "0.5rem",
                  borderRadius: "calc(var(--radius-md) - 2px)",
                  border: "none",
                  background: cameraMode
                    ? "var(--color-card)"
                    : "transparent",
                  color: cameraMode
                    ? "var(--color-foreground)"
                    : "var(--color-muted-foreground)",
                  fontSize: "0.85rem",
                  fontWeight: cameraMode ? 600 : 400,
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "0.5rem",
                }}
              >
                <Camera size={16} />
                Camera Scan
              </button>
            </div>

            {cameraMode ? (
              <QRScanner
                onResult={handleScan}
                onError={(err) => showToast(err, "error")}
              />
            ) : (
              <form onSubmit={handleManualSubmit}>
                <div style={{ marginBottom: "1rem" }}>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.8rem",
                      fontWeight: 600,
                      marginBottom: "0.5rem",
                    }}
                  >
                    Release Code / UUID
                  </label>
                  <input
                    type="text"
                    value={manualInput}
                    onChange={(e) => setManualInput(e.target.value)}
                    placeholder="Enter your document release code"
                    style={{
                      width: "100%",
                      padding: "0.75rem 1rem",
                      background: "var(--color-input)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-md)",
                      color: "var(--color-foreground)",
                      fontSize: "0.95rem",
                      outline: "none",
                      fontFamily: "var(--font-mono)",
                    }}
                  />
                </div>
                <button
                  type="submit"
                  style={{ ...btnStyle("primary"), width: "100%", justifyContent: "center" }}
                >
                  <Search size={16} /> Look Up Document
                </button>
              </form>
            )}
          </div>
        )}
      </div>

      {/* Footer */}
      <div
        style={{
          marginTop: "2rem",
          fontSize: "0.75rem",
          color: "var(--color-muted-foreground)",
          textAlign: "center",
        }}
      >
        Powered by PrintFlow
      </div>

      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
