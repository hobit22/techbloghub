/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      colors: {
        background: 'var(--background)',
        foreground: 'var(--foreground)',
        primary: {
          DEFAULT: 'var(--primary)',
          dark: 'var(--primary-dark)',
          light: 'var(--primary-light)',
        },
        success: {
          DEFAULT: 'var(--success)',
          light: 'var(--success-light)',
        },
        warning: {
          DEFAULT: 'var(--warning)',
          light: 'var(--warning-light)',
        },
        error: {
          DEFAULT: 'var(--error)',
          light: 'var(--error-light)',
        },
      },
    },
  },
  plugins: [],
}