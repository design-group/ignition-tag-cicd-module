import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    "index",
    {
      type: 'category',
      label: 'Introduction',
      collapsed: false,
      items: [
        'introduction/key-features',
        'introduction/use-cases',
      ],
    },
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/installation',
        'getting-started/basic-setup',
        'getting-started/configuration-file',
        'getting-started/configuration-schema',
      ],
    },
    {
      type: 'category',
      label: 'Designer Features',
      items: [
        'designer/ui-components',
      ],
    },
    {
      type: 'category',
      label: 'Tag Export',
      items: [
        'tag-export/export-modes',
      ],
    },
  ],
};

export default sidebars;