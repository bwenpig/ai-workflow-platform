// 项目文件服务
export interface ProjectFile {
  schemaVersion: number;
  project: {
    id: string;
    name: string;
    version: string;
    description: string;
    createdAt: string;
    updatedAt: string;
  };
  workflow: any;
}

export const projectService = {
  // 新建项目
  newProject: (): ProjectFile => ({
    schemaVersion: 1,
    project: {
      id: `proj_${Date.now()}`,
      name: '未命名项目',
      version: '1.0.0',
      description: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    workflow: { nodes: [], edges: [], viewport: { x: 0, y: 0, zoom: 1 } }
  }),

  // 保存到文件
  saveProject: async (project: ProjectFile) => {
    const json = JSON.stringify(project, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${project.project.name}.awf.json`;
    a.click();
    URL.revokeObjectURL(url);
  },

  // 从文件加载
  loadProject: (file: File): Promise<ProjectFile> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = JSON.parse(e.target?.result as string);
          resolve(data);
        } catch (err) {
          reject(err);
        }
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }
};