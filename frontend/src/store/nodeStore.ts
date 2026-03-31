import { create } from 'zustand';
import { Node } from '@xyflow/react';

interface NodeState {
  nodes: Node[];
  selectedNodeIds: string[];
  addNode: (node: Node) => void;
  removeNode: (nodeId: string) => void;
  updateNode: (nodeId: string, updates: Partial<Node>) => void;
  selectNode: (nodeId: string, multi?: boolean) => void;
  deselectNode: (nodeId: string) => void;
  clearSelection: () => void;
  removeSelectedNodes: () => void;
}

export const useNodeStore = create<NodeState>((set, get) => ({
  nodes: [],
  selectedNodeIds: [],
  
  addNode: (node) =>
    set((state) => ({ nodes: [...state.nodes, node] })),
  
  removeNode: (nodeId) =>
    set((state) => ({
      nodes: state.nodes.filter((n) => n.id !== nodeId),
      selectedNodeIds: state.selectedNodeIds.filter((id) => id !== nodeId),
    })),
  
  updateNode: (nodeId, updates) =>
    set((state) => ({
      nodes: state.nodes.map((node) =>
        node.id === nodeId ? { ...node, ...updates } : node
      ),
    })),
  
  selectNode: (nodeId, multi = false) =>
    set((state) => ({
      selectedNodeIds: multi
        ? [...state.selectedNodeIds, nodeId]
        : [nodeId],
    })),
  
  deselectNode: (nodeId) =>
    set((state) => ({
      selectedNodeIds: state.selectedNodeIds.filter((id) => id !== nodeId),
    })),
  
  clearSelection: () => set({ selectedNodeIds: [] }),
  
  removeSelectedNodes: () =>
    set((state) => ({
      nodes: state.nodes.filter(
        (node) => !state.selectedNodeIds.includes(node.id)
      ),
      selectedNodeIds: [],
    })),
}));
