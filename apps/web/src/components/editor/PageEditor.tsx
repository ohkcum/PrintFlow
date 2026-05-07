"use client";

import { useEffect, useRef, useCallback, useState, forwardRef, useImperativeHandle } from "react";
import {
  Canvas as FabricCanvas,
  PencilBrush,
  Rect,
  Circle,
  Triangle,
  Line,
  Textbox,
  ActiveSelection,
  FabricImage,
  FabricObject,
  loadSVGFromString,
  type Canvas as FabricCanvasType,
} from "fabric";

// ─── Fabric.js v6 Setup ─────────────────────────────────────────────────────────

// Patch fabric object defaults to match PrintFlowLite behavior
FabricObject.prototype.set = function (
  this: FabricObject,
  values: Record<string, unknown>,
  options?: { silent?: boolean; renderOnAddRemove?: boolean }
) {
  const proto = Object.getPrototypeOf(this);
  for (const key of Object.keys(values)) {
    const setterName = `set${key.charAt(0).toUpperCase() + key.slice(1)}`;
    const setter = (proto as any)[setterName];
    if (typeof setter === "function") {
      setter.call(this, values[key]);
    } else {
      (this as any)[key] = values[key];
    }
  }
  if (!options?.silent) this.setCoords();
  if (options?.renderOnAddRemove !== false && this.canvas) {
    this.canvas?.requestRenderAll();
  }
  return this;
};

// Default stroke scaling to match PrintFlowLite's _strokeUniform = false behavior
// Note: strokeUniform defaults to false in Fabric.js v6, matching PrintFlowLite behavior

// ─── Types ─────────────────────────────────────────────────────────────────────

export interface CanvasEditorHandle {
  addRect: () => void;
  addCircle: () => void;
  addTriangle: () => void;
  addLine: () => void;
  addTextbox: (text?: string) => void;
  enableDrawingMode: (enable: boolean) => void;
  setBrushColor: (color: string) => void;
  setBrushWidth: (width: number) => void;
  setFill: (color: string | null) => void;
  setStroke: (color: string | null) => void;
  setStrokeWidth: (width: number) => void;
  setOpacity: (value: number) => void;
  setZoom: (factor: number) => void;
  zoomIn: () => void;
  zoomOut: () => void;
  unZoom: () => number;
  getZoom: () => number;
  toSVG: () => string;
  toJSON: () => string;
  loadSVG: (svg: string) => Promise<void>;
  loadJSON: (json: string) => void;
  clear: () => void;
  clearSelection: () => void;
  selectAll: () => void;
  countObjects: () => number;
  hasTextObjects: () => boolean;
  deactivateAll: () => void;
  setBackgroundImage: (url: string) => Promise<void>;
  setCanvasSize: (width: number, height: number) => void;
}

export interface PageEditorProps {
  canvasId?: string;
  width?: number;
  height?: number;
  backgroundImage?: string;
  onAfterRender?: (count: number) => void;
  onSelectionCreated?: (count: number) => void;
  onSelectionCleared?: () => void;
  onSelectionUpdated?: (count: number) => void;
  readonly?: boolean;
}

// ─── Constants (matching PrintFlowLite) ────────────────────────────────────────

const ADD_L_INIT = 50;
const ADD_T_INIT = 50;
const ADD_INC = 20;
const ZOOM_STEP = 1.1;
const DEFAULT_FILL = "rgb(255,255,255)";
const DEFAULT_STROKE = "rgb(0,0,0)";
const DEFAULT_STROKE_WIDTH = 0;
const DEFAULT_OPACITY = 1.0;
const DEFAULT_BRUSH_COLOR = "rgb(0,0,0)";
const DEFAULT_BRUSH_WIDTH = 2;
const ZOOM_MIN = 0.1;
const ZOOM_MAX = 10;

// ─── Helper: Fix stroke width on scaled objects (PrintFlowLite behavior) ───────

function fixStrokeWidthOnScale(obj: FabricObject) {
  const scaleX = obj.scaleX ?? 1;
  const scaleY = obj.scaleY ?? 1;
  const strokeWidth = (obj as any).strokeWidth ?? 0;
  if (scaleX !== 1 || scaleY !== 1) {
    const newStroke = strokeWidth / Math.max(scaleX, scaleY);
    (obj as any).strokeWidth = newStroke;
    obj.set({ scaleX: 1, scaleY: 1 });
    obj.setCoords();
  }
}

// ─── Component ─────────────────────────────────────────────────────────────────

export const PageEditor = forwardRef<CanvasEditorHandle, PageEditorProps>(
  (
    {
      canvasId,
      width = 800,
      height = 600,
      backgroundImage,
      onAfterRender,
      onSelectionCreated,
      onSelectionCleared,
      onSelectionUpdated,
      readonly = false,
    },
    ref
  ) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const fabricRef = useRef<FabricCanvasType | null>(null);
    const brushRef = useRef<PencilBrush | null>(null);

    const stateRef = useRef({
      zoom: 1,
      fill: DEFAULT_FILL,
      stroke: DEFAULT_STROKE,
      strokeWidth: DEFAULT_STROKE_WIDTH,
      fillTextbox: "rgb(0,0,0)",
      opacity: DEFAULT_OPACITY,
      brushColor: DEFAULT_BRUSH_COLOR,
      brushWidth: DEFAULT_BRUSH_WIDTH,
      isDrawingMode: false,
      coordAddObj: { left: ADD_L_INIT, top: ADD_T_INIT },
      backgroundImageUrl: "",
    });

    // ─── Canvas Initialization ────────────────────────────────────────────────

    useEffect(() => {
      if (!canvasRef.current) return;

      const canvas = new FabricCanvas(canvasRef.current, {
        width,
        height,
        backgroundColor: "#ffffff",
        selection: !readonly,
        fireRightClick: false,
        stopContextMenu: false,
      });

      fabricRef.current = canvas;

      // ─── Free drawing brush ────────────────────────────────────────────────

      const brush = new PencilBrush(canvas);
      brush.color = stateRef.current.brushColor;
      brush.width = stateRef.current.brushWidth;
      brushRef.current = brush;

      // ─── Event Listeners (matching PrintFlowLite) ─────────────────────────

      canvas.on("path:created", (e: any) => {
        const path = e.path;
        if (path) {
          path.set({
            stroke: stateRef.current.stroke || stateRef.current.brushColor,
            strokeWidth: stateRef.current.strokeWidth,
            fill: null,
          });
          onAfterRender?.(canvas.getObjects().length);
        }
      });

      canvas.on("selection:created", (e: any) => {
        const n = e.selected?.length ?? 0;
        onSelectionCreated?.(n);
      });

      canvas.on("selection:cleared", () => {
        onSelectionCleared?.();
      });

      canvas.on("selection:updated", (e: any) => {
        const n = e.selected?.length ?? 0;
        onSelectionUpdated?.(n);
      });

      canvas.on("after:render", () => {
        onAfterRender?.(canvas.getObjects().length);
      });

      // Stroke width fix on scaling (PrintFlowLite behavior)
      canvas.on("object:scaling", (e: any) => {
        const obj = e.target;
        if (obj instanceof Rect || obj instanceof Line) {
          fixStrokeWidthOnScale(obj);
        }
      });

      // ─── Load background image if provided ────────────────────────────────

      if (backgroundImage) {
        loadBackgroundImage(canvas, backgroundImage);
      }

      return () => {
        canvas.dispose();
        fabricRef.current = null;
      };
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ─── Background Image Loading ──────────────────────────────────────────

    const loadBackgroundImage = useCallback(
      async (canvas: FabricCanvasType, url: string) => {
        try {
      const img = new window.Image();
      img.crossOrigin = "anonymous";
      await new Promise<void>((resolve, reject) => {
        img.onload = () => resolve();
        img.onerror = () => reject(new Error("Failed to load image"));
        img.src = url;
      });

      const fabricImg = await FabricImage.fromURL(url, {
        crossOrigin: "anonymous",
      });

          fabricImg.set({
            originX: "left",
            originY: "top",
            excludeFromExport: true,
          });

          // Scale to fit canvas
          const scaleX = canvas.width! / (fabricImg.width ?? 1);
          const scaleY = canvas.height! / (fabricImg.height ?? 1);
          const scale = Math.min(scaleX, scaleY);
          fabricImg.scale(scale);

          canvas.backgroundImage = fabricImg;
          canvas.requestRenderAll();
          stateRef.current.backgroundImageUrl = url;
        } catch {
          // Ignore background load errors
        }
      },
      []
    );

    useEffect(() => {
      if (fabricRef.current && backgroundImage) {
        loadBackgroundImage(fabricRef.current, backgroundImage);
      }
    }, [backgroundImage, loadBackgroundImage]);

    // ─── Shape: Rect ──────────────────────────────────────────────────────

    const addRect = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;

      const c = stateRef.current.coordAddObj;
      c.left += ADD_INC;
      c.top += ADD_INC;

      const rect = new Rect({
        left: c.left,
        top: c.top,
        width: 100,
        height: 80,
        fill: stateRef.current.fill,
        stroke: stateRef.current.stroke || undefined,
        strokeWidth: stateRef.current.strokeWidth,
        opacity: stateRef.current.opacity,
        strokeUniform: false,
        selectable: !readonly,
        rx: 0,
        ry: 0,
      });

      canvas.add(rect);
      canvas.setActiveObject(rect);
      canvas.requestRenderAll();
      onAfterRender?.(canvas.getObjects().length);
    }, [readonly, onAfterRender]);

    // ─── Shape: Circle ────────────────────────────────────────────────────

    const addCircle = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;

      const c = stateRef.current.coordAddObj;
      c.left += ADD_INC;
      c.top += ADD_INC;

      const circle = new Circle({
        left: c.left,
        top: c.top,
        radius: 50,
        fill: stateRef.current.fill,
        stroke: stateRef.current.stroke || undefined,
        strokeWidth: stateRef.current.strokeWidth,
        opacity: stateRef.current.opacity,
        selectable: !readonly,
      });

      canvas.add(circle);
      canvas.setActiveObject(circle);
      canvas.requestRenderAll();
      onAfterRender?.(canvas.getObjects().length);
    }, [readonly, onAfterRender]);

    // ─── Shape: Triangle ──────────────────────────────────────────────────

    const addTriangle = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;

      const c = stateRef.current.coordAddObj;
      c.left += ADD_INC;
      c.top += ADD_INC;

      const triangle = new Triangle({
        left: c.left,
        top: c.top,
        width: 100,
        height: 80,
        fill: stateRef.current.fill,
        stroke: stateRef.current.stroke || undefined,
        strokeWidth: stateRef.current.strokeWidth,
        opacity: stateRef.current.opacity,
        selectable: !readonly,
      });

      canvas.add(triangle);
      canvas.setActiveObject(triangle);
      canvas.requestRenderAll();
      onAfterRender?.(canvas.getObjects().length);
    }, [readonly, onAfterRender]);

    // ─── Shape: Line ──────────────────────────────────────────────────────

    const addLine = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;

      const c = stateRef.current.coordAddObj;
      c.left += ADD_INC;
      c.top += ADD_INC;

      const line = new Line([c.left, c.top, c.left + 150, c.top], {
        stroke: stateRef.current.stroke || "rgb(0,0,0)",
        strokeWidth: stateRef.current.strokeWidth || 2,
        selectable: !readonly,
      });

      canvas.add(line);
      canvas.setActiveObject(line);
      canvas.requestRenderAll();
      onAfterRender?.(canvas.getObjects().length);
    }, [readonly, onAfterRender]);

    // ─── Shape: Textbox ───────────────────────────────────────────────────

    const addTextbox = useCallback(
      (text = "Text") => {
        const canvas = fabricRef.current;
        if (!canvas) return;

        const c = stateRef.current.coordAddObj;
        c.left += ADD_INC;
        c.top += ADD_INC;

        const textbox = new Textbox(text, {
          left: c.left,
          top: c.top,
          width: 200,
          fontSize: 18,
          fontFamily: "Inter, Arial, sans-serif",
          fill: stateRef.current.fillTextbox,
          stroke: undefined,
          strokeWidth: 0,
          opacity: stateRef.current.opacity,
          selectable: !readonly,
          editable: !readonly,
          wordWrap: true,
          textAlign: "left",
        });

        canvas.add(textbox);
        canvas.setActiveObject(textbox);
        canvas.requestRenderAll();
        onAfterRender?.(canvas.getObjects().length);
      },
      [readonly, onAfterRender]
    );

    // ─── Drawing Mode ─────────────────────────────────────────────────────

    const enableDrawingMode = useCallback((enable: boolean) => {
      const canvas = fabricRef.current;
      if (!canvas) return;

      if (enable) {
        canvas.freeDrawingBrush = brushRef.current ?? undefined;
        canvas.isDrawingMode = true;
        stateRef.current.isDrawingMode = true;
      } else {
        canvas.isDrawingMode = false;
        stateRef.current.isDrawingMode = false;
      }
      canvas.requestRenderAll();
    }, []);

    // ─── Brush Controls ──────────────────────────────────────────────────

    const setBrushColor = useCallback((color: string) => {
      stateRef.current.brushColor = color;
      if (brushRef.current) {
        brushRef.current.color = color;
      }
    }, []);

    const setBrushWidth = useCallback((width: number) => {
      stateRef.current.brushWidth = width;
      if (brushRef.current) {
        brushRef.current.width = width;
      }
    }, []);

    // ─── Fill / Stroke ───────────────────────────────────────────────────

    const setFill = useCallback((color: string | null) => {
      stateRef.current.fill = color ?? DEFAULT_FILL;
      const canvas = fabricRef.current;
      if (!canvas) return;
      const active = canvas.getActiveObject();
      if (active) {
        active.set("fill", color ?? DEFAULT_FILL);
        canvas.requestRenderAll();
      }
    }, []);

    const setStroke = useCallback((color: string | null) => {
      stateRef.current.stroke = color ?? DEFAULT_STROKE;
      const canvas = fabricRef.current;
      if (!canvas) return;
      const active = canvas.getActiveObject();
      if (active) {
        active.set("stroke", color ?? undefined);
        active.set("strokeWidth", color ? (stateRef.current.strokeWidth || 1) : 0);
        canvas.requestRenderAll();
      }
    }, []);

    const setStrokeWidth = useCallback((width: number) => {
      stateRef.current.strokeWidth = width;
      const canvas = fabricRef.current;
      if (!canvas) return;
      const active = canvas.getActiveObject();
      if (active) {
        active.set("strokeWidth", width);
        canvas.requestRenderAll();
      }
    }, []);

    const setOpacity = useCallback((value: number) => {
      stateRef.current.opacity = Math.max(0, Math.min(1, value));
      const canvas = fabricRef.current;
      if (!canvas) return;
      const active = canvas.getActiveObject();
      if (active) {
        active.set("opacity", stateRef.current.opacity);
        canvas.requestRenderAll();
      }
    }, []);

    // ─── Zoom ────────────────────────────────────────────────────────────

    const setZoom = useCallback((factor: number) => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      const clamped = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, factor));
      canvas.setZoom(clamped);
      stateRef.current.zoom = clamped;
      canvas.setWidth(width * clamped);
      canvas.setHeight(height * clamped);
      canvas.requestRenderAll();
    }, [width, height]);

    const zoomIn = useCallback(() => {
      setZoom(stateRef.current.zoom * ZOOM_STEP);
    }, [setZoom]);

    const zoomOut = useCallback(() => {
      setZoom(stateRef.current.zoom / ZOOM_STEP);
    }, [setZoom]);

    const unZoom = useCallback(() => {
      const prev = stateRef.current.zoom;
      setZoom(1);
      return prev;
    }, [setZoom]);

    const getZoom = useCallback(() => stateRef.current.zoom, []);

    // ─── Export ──────────────────────────────────────────────────────────

    const toSVG = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return "";
      return canvas.toSVG();
    }, []);

    const toJSON = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return "";
      return JSON.stringify(canvas.toJSON());
    }, []);

    // ─── Import ──────────────────────────────────────────────────────────

    const loadJSON = useCallback((json: string) => {
      const canvas = fabricRef.current;
      if (!canvas || !json) return;
      canvas.loadFromJSON(json).then(() => {
        canvas.requestRenderAll();
        onAfterRender?.(canvas.getObjects().length);
      });
    }, [onAfterRender]);

    const loadSVG = useCallback(async (svg: string) => {
      const canvas = fabricRef.current;
      if (!canvas || !svg) return;
      try {
        const result = await loadSVGFromString(svg);
        if (result.objects.length > 0) {
          canvas.add(...(result.objects as FabricObject[]).filter(Boolean));
          canvas.requestRenderAll();
          onAfterRender?.(canvas.getObjects().length);
        }
      } catch {
        // Ignore SVG load errors
      }
    }, [onAfterRender]);

    // ─── Utilities ────────────────────────────────────────────────────────

    const clear = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      canvas.clear();
      if (stateRef.current.backgroundImageUrl) {
        loadBackgroundImage(canvas, stateRef.current.backgroundImageUrl);
      }
      canvas.requestRenderAll();
      onAfterRender?.(0);
    }, [loadBackgroundImage, onAfterRender]);

    const clearSelection = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      canvas.discardActiveObject();
      canvas.requestRenderAll();
    }, []);

    const selectAll = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      const objs = canvas.getObjects().filter((o) => o.selectable !== false);
      if (objs.length > 0) {
        canvas.setActiveObject(new ActiveSelection(objs, { canvas }));
        canvas.requestRenderAll();
      }
    }, []);

    const countObjects = useCallback(() => {
      const canvas = fabricRef.current;
      return canvas?.getObjects().length ?? 0;
    }, []);

    const hasTextObjects = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return false;
      return canvas.getObjects().some((o) => o instanceof Textbox);
    }, []);

    const deactivateAll = useCallback(() => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      canvas.discardActiveObject();
      canvas.requestRenderAll();
    }, []);

    const setCanvasSize = useCallback((w: number, h: number) => {
      const canvas = fabricRef.current;
      if (!canvas) return;
      canvas.setWidth(w);
      canvas.setHeight(h);
      canvas.requestRenderAll();
    }, []);

    // ─── Expose public API via ref ───────────────────────────────────────

    useImperativeHandle(ref, () => ({
      addRect,
      addCircle,
      addTriangle,
      addLine,
      addTextbox,
      enableDrawingMode,
      setBrushColor,
      setBrushWidth,
      setFill,
      setStroke,
      setStrokeWidth,
      setOpacity,
      setZoom,
      zoomIn,
      zoomOut,
      unZoom,
      getZoom,
      toSVG,
      toJSON,
      loadSVG,
      loadJSON,
      clear,
      clearSelection,
      selectAll,
      countObjects,
      hasTextObjects,
      deactivateAll,
      setBackgroundImage: (url: string) =>
        fabricRef.current ? loadBackgroundImage(fabricRef.current, url) : Promise.resolve(),
      setCanvasSize,
    }));

    return (
      <canvas
        ref={canvasRef}
        id={canvasId}
        style={{
          display: "block",
          border: "1px solid var(--color-border)",
          borderRadius: "var(--radius-md)",
          maxWidth: "100%",
        }}
      />
    );
  }
);

PageEditor.displayName = "PageEditor";

// ─── Toolbar Component ────────────────────────────────────────────────────────────

// Separate Toolbar component that wraps the editor with controls
export interface PageEditorToolbarProps {
  editorRef: React.RefObject<CanvasEditorHandle | null>;
}

export function PageEditorToolbar({ editorRef }: PageEditorToolbarProps) {
  const [fillColor, setFillColor] = useState("#ffffff");
  const [strokeColor, setStrokeColor] = useState("#000000");
  const [strokeWidth, setStrokeWidth] = useState(2);
  const [brushColor, setBrushColor] = useState("#000000");
  const [brushWidth, setBrushWidth] = useState(2);
  const [isDrawing, setIsDrawing] = useState(false);
  const [objectCount, setObjectCount] = useState(0);
  const [hasSelection, setHasSelection] = useState(false);
  const [zoom, setZoomState] = useState(1);

  useEffect(() => {
    const editor = editorRef.current;
    if (!editor) return;

    // Set up callbacks
    const id = setInterval(() => {
      setObjectCount(editor.countObjects());
      setZoomState(editor.getZoom());
    }, 500);

    return () => clearInterval(id);
  }, [editorRef]);

  function dispatch(action: string, value?: any) {
    const editor = editorRef.current;
    if (!editor) return;
    switch (action) {
      case "rect": editor.addRect(); break;
      case "circle": editor.addCircle(); break;
      case "triangle": editor.addTriangle(); break;
      case "line": editor.addLine(); break;
      case "text": editor.addTextbox(); break;
      case "draw": {
        const next = !isDrawing;
        setIsDrawing(next);
        editor.enableDrawingMode(next);
        break;
      }
      case "brushColor": {
        setBrushColor(value);
        editor.setBrushColor(value);
        break;
      }
      case "brushWidth": {
        setBrushWidth(value);
        editor.setBrushWidth(value);
        break;
      }
      case "fill": {
        setFillColor(value);
        editor.setFill(value);
        break;
      }
      case "fillNull": editor.setFill(null); break;
      case "stroke": {
        setStrokeColor(value);
        editor.setStroke(value);
        break;
      }
      case "strokeWidth": {
        setStrokeWidth(value);
        editor.setStrokeWidth(value);
        break;
      }
      case "clear": editor.clear(); break;
      case "undo": editor.clear(); break; // reload from server in real impl
      case "zoomin": editor.zoomIn(); break;
      case "zoomout": editor.zoomOut(); break;
      case "selectAll": editor.selectAll(); break;
    }
  }

  const btnStyle = (active?: boolean): React.CSSProperties => ({
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: "36px",
    height: "36px",
    borderRadius: "var(--radius-md)",
    border: `1px solid ${active ? "var(--color-primary)" : "var(--color-border)"}`,
    background: active ? "oklch(0.72 0.18 250 / 0.1)" : "var(--color-input)",
    color: active ? "var(--color-primary)" : "var(--color-muted-foreground)",
    cursor: "pointer",
    fontSize: "0.75rem",
    fontWeight: active ? 600 : 400,
    transition: "all 0.15s",
  });

  const divider = (
    <div style={{ width: "1px", height: "24px", background: "var(--color-border)", margin: "0 4px" }} />
  );

  return (
    <div
      style={{
        display: "flex",
        flexWrap: "wrap",
        gap: "4px",
        alignItems: "center",
        padding: "0.5rem 0.75rem",
        background: "var(--color-card)",
        border: "1px solid var(--color-border)",
        borderRadius: "var(--radius-md)",
        marginBottom: "0.5rem",
      }}
    >
      {/* Shapes */}
      {[
        { label: "R", title: "Rectangle", action: "rect" },
        { label: "C", title: "Circle", action: "circle" },
        { label: "T", title: "Triangle", action: "triangle" },
        { label: "L", title: "Line", action: "line" },
        { label: "A", title: "Text", action: "text" },
      ].map(({ label, title, action }) => (
        <button key={action} title={title} onClick={() => dispatch(action)} style={btnStyle()}>
          {label}
        </button>
      ))}

      {divider}

      {/* Brush */}
      <button
        title="Freehand Drawing"
        onClick={() => dispatch("draw")}
        style={btnStyle(isDrawing)}
      >
        ✏️
      </button>
      {isDrawing && (
        <>
          <input
            type="color"
            value={brushColor}
            onChange={(e) => dispatch("brushColor", e.target.value)}
            title="Brush Color"
            style={{ width: "28px", height: "28px", padding: "2px", border: "1px solid var(--color-border)", borderRadius: "4px", cursor: "pointer", background: "none" }}
          />
          <input
            type="range"
            min={1}
            max={20}
            value={brushWidth}
            onChange={(e) => dispatch("brushWidth", parseInt(e.target.value))}
            title="Brush Width"
            style={{ width: "60px", cursor: "pointer" }}
          />
        </>
      )}

      {divider}

      {/* Fill */}
      <input
        type="color"
        value={fillColor}
        onChange={(e) => dispatch("fill", e.target.value)}
        title="Fill Color"
        style={{ width: "28px", height: "28px", padding: "2px", border: "1px solid var(--color-border)", borderRadius: "4px", cursor: "pointer", background: "none" }}
      />
      <button
        title="Transparent Fill"
        onClick={() => dispatch("fillNull")}
        style={{ ...btnStyle(), fontSize: "0.65rem", width: "28px", height: "28px" }}
      >
        ∅
      </button>

      {/* Stroke */}
      <input
        type="color"
        value={strokeColor}
        onChange={(e) => dispatch("stroke", e.target.value)}
        title="Stroke Color"
        style={{ width: "28px", height: "28px", padding: "2px", border: "1px solid var(--color-border)", borderRadius: "4px", cursor: "pointer", background: "none" }}
      />
      <input
        type="number"
        min={0}
        max={20}
        value={strokeWidth}
        onChange={(e) => dispatch("strokeWidth", parseInt(e.target.value) || 0)}
        title="Stroke Width"
        style={{ width: "50px", padding: "4px 6px", border: "1px solid var(--color-border)", borderRadius: "4px", background: "var(--color-input)", color: "var(--color-foreground)", fontSize: "0.75rem", outline: "none" }}
      />

      {divider}

      {/* Actions */}
      <button title="Select All" onClick={() => dispatch("selectAll")} style={btnStyle()}>
        ⊞
      </button>
      <button title="Clear All" onClick={() => dispatch("clear")} style={btnStyle()}>
        🗑
      </button>

      {divider}

      {/* Zoom */}
      <button title="Zoom Out" onClick={() => dispatch("zoomout")} style={btnStyle()}>
        −
      </button>
      <span style={{ fontSize: "0.75rem", color: "var(--color-muted-foreground)", minWidth: "40px", textAlign: "center", fontFamily: "var(--font-mono)" }}>
        {Math.round(zoom * 100)}%
      </span>
      <button title="Zoom In" onClick={() => dispatch("zoomin")} style={btnStyle()}>
        +
      </button>

      {/* Object count */}
      <div style={{ marginLeft: "auto", fontSize: "0.75rem", color: "var(--color-muted-foreground)" }}>
        {objectCount} objects
      </div>
    </div>
  );
}
