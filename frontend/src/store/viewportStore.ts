import { create } from 'zustand';

interface ViewportState {
  zoom: number;
  pan: { x: number; y: number };
  setZoom: (zoom: number) => void;
  setPan: (pan: { x: number; y: number }) => void;
  updateViewport: (viewport: { zoom: number; pan: { x: number; y: number } }) => void;
  fitView: () => void;
  resetViewport: () => void;
}

export const useViewportStore = create<ViewportState>((set) => ({
  zoom: 1,
  pan: { x: 0, y: 0 },
  
  setZoom: (zoom) =>
    set((state) => ({
      zoom: Math.min(Math.max(zoom, 0.1), 2),
    })),
  
  setPan: (pan) => set({ pan }),
  
  updateViewport: (viewport) => set(viewport),
  
  fitView: () =>
    set({
      zoom: 1,
      pan: { x: 0, y: 0 },
    }),
  
  resetViewport: () =>
    set({
      zoom: 1,
      pan: { x: 0, y: 0 },
    }),
}));
