import { create } from 'zustand';

export interface NodeStatus {
  nodeId: string;
  status: 'idle' | 'pending' | 'running' | 'success' | 'error' | 'skipped';
  progress?: number;
  message?: string;
  timestamp?: string;
}

interface NodeStatusStore {
  statuses: Record<string, NodeStatus>;
  updateStatus: (nodeId: string, status: NodeStatus) => void;
  updateProgress: (nodeId: string, progress: number) => void;
  reset: () => void;
  resetNode: (nodeId: string) => void;
}

export const useNodeStatusStore = create<NodeStatusStore>((set) => ({
  statuses: {},
  
  updateStatus: (nodeId, status) => 
    set(state => ({ 
      statuses: { ...state.statuses, [nodeId]: status } 
    })),
  
  updateProgress: (nodeId, progress) =>
    set(state => ({
      statuses: {
        ...state.statuses,
        [nodeId]: { 
          ...state.statuses[nodeId], 
          progress,
          status: 'running' as const 
        }
      }
    })),
  
  reset: () => set({ statuses: {} }),
  
  resetNode: (nodeId) =>
    set(state => {
      const { [nodeId]: _, ...rest } = state.statuses;
      return { statuses: rest };
    })
}));