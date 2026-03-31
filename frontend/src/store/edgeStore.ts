import { create } from 'zustand';
import { Edge } from '@xyflow/react';

interface EdgeState {
  edges: Edge[];
  selectedEdgeIds: string[];
  addEdge: (edge: Edge) => void;
  removeEdge: (edgeId: string) => void;
  selectEdge: (edgeId: string, multi?: boolean) => void;
  deselectEdge: (edgeId: string) => void;
  clearSelection: () => void;
  removeSelectedEdges: () => void;
  removeEdgesForNode: (nodeId: string) => void;
}

export const useEdgeStore = create<EdgeState>((set, get) => ({
  edges: [],
  selectedEdgeIds: [],
  
  addEdge: (edge) =>
    set((state) => ({ edges: [...state.edges, edge] })),
  
  removeEdge: (edgeId) =>
    set((state) => ({
      edges: state.edges.filter((e) => e.id !== edgeId),
      selectedEdgeIds: state.selectedEdgeIds.filter((id) => id !== edgeId),
    })),
  
  selectEdge: (edgeId, multi = false) =>
    set((state) => ({
      selectedEdgeIds: multi
        ? [...state.selectedEdgeIds, edgeId]
        : [edgeId],
    })),
  
  deselectEdge: (edgeId) =>
    set((state) => ({
      selectedEdgeIds: state.selectedEdgeIds.filter((id) => id !== edgeId),
    })),
  
  clearSelection: () => set({ selectedEdgeIds: [] }),
  
  removeSelectedEdges: () =>
    set((state) => ({
      edges: state.edges.filter(
        (edge) => !state.selectedEdgeIds.includes(edge.id)
      ),
      selectedEdgeIds: [],
    })),
  
  removeEdgesForNode: (nodeId) =>
    set((state) => ({
      edges: state.edges.filter(
        (edge) => edge.source !== nodeId && edge.target !== nodeId
      ),
    })),
}));
