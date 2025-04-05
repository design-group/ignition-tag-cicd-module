import { themes as prismThemes } from 'prism-react-renderer';
import type { Config } from '@docusaurus/types';

const config: Config = {
  title: 'Ignition Tag CICD Module',
  tagline: 'Enabling Version control and CI/CD practices for Ignition tags',
  favicon: 'img/favicon.ico',
  url: 'https://design-group.github.io',
  baseUrl: '/ignition-tag-cicd-module/',
  organizationName: 'design-group',
  projectName: 'ignition-tag-cicd-module',
  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: '/',
          editUrl:
            'https://github.com/design-group/ignition-tag-cicd-module/tree/master/docs',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./static/css/custom.css'),
        },
      },
    ],
  ],

  markdown: {
    mermaid: true,
  },

  themes: ['@docusaurus/theme-mermaid'],

  themeConfig: {
    navbar: {
      title: 'Ignition Tag CICD Module',
      logo: {
        alt: 'Ignition Logo',
        src: 'img/Logo-Ignition-Check.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          href: 'https://github.com/design-group/ignition-tag-cicd-module',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/getting-started/installation',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'GitHub Issues',
              href: 'https://github.com/design-group/ignition-tag-cicd-module/issues',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/design-group/ignition-tag-cicd-module',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Barry-Wehmiller Design Group. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'json', 'bash', 'groovy'],
    },
    mermaid: {
      theme: { light: 'neutral', dark: 'dark' },
    },
  },
};

export default config;