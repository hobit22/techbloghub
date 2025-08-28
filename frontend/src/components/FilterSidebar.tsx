'use client';

import { Blog, Tag, Category } from '@/types';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { useState } from 'react';

interface FilterSidebarProps {
  blogs: Blog[];
  tags: Tag[];
  categories: Category[];
  selectedCompanies: string[];
  selectedTags: string[];
  selectedCategories: string[];
  onCompanyChange: (companies: string[]) => void;
  onTagChange: (tags: string[]) => void;
  onCategoryChange: (categories: string[]) => void;
}

export default function FilterSidebar({
  blogs,
  tags,
  categories,
  selectedCompanies,
  selectedTags,
  selectedCategories,
  onCompanyChange,
  onTagChange,
  onCategoryChange,
}: FilterSidebarProps) {
  const [expandedSections, setExpandedSections] = useState({
    companies: true,
    categories: true,
    tags: false,
  });

  const toggleSection = (section: keyof typeof expandedSections) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  const handleCompanyToggle = (company: string) => {
    if (selectedCompanies.includes(company)) {
      onCompanyChange(selectedCompanies.filter(c => c !== company));
    } else {
      onCompanyChange([...selectedCompanies, company]);
    }
  };

  const handleTagToggle = (tag: string) => {
    if (selectedTags.includes(tag)) {
      onTagChange(selectedTags.filter(t => t !== tag));
    } else {
      onTagChange([...selectedTags, tag]);
    }
  };

  const handleCategoryToggle = (category: string) => {
    if (selectedCategories.includes(category)) {
      onCategoryChange(selectedCategories.filter(c => c !== category));
    } else {
      onCategoryChange([...selectedCategories, category]);
    }
  };

  const companies = Array.from(new Set(blogs.map(blog => blog.company))).sort();
  const topTags = tags.slice(0, 20);

  return (
    <div className="w-64 bg-white shadow-sm border-r h-screen overflow-y-auto">
      <div className="p-4">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">필터</h2>

        <div className="space-y-6">
          <div>
            <button
              onClick={() => toggleSection('companies')}
              className="flex items-center justify-between w-full text-sm font-medium text-gray-700 mb-3"
            >
              <span>회사</span>
              {expandedSections.companies ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
            {expandedSections.companies && (
              <div className="space-y-2">
                {companies.map((company) => (
                  <label key={company} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedCompanies.includes(company)}
                      onChange={() => handleCompanyToggle(company)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">{company}</span>
                  </label>
                ))}
              </div>
            )}
          </div>

          <div>
            <button
              onClick={() => toggleSection('categories')}
              className="flex items-center justify-between w-full text-sm font-medium text-gray-700 mb-3"
            >
              <span>카테고리</span>
              {expandedSections.categories ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
            {expandedSections.categories && (
              <div className="space-y-2">
                {categories.map((category) => (
                  <label key={category.id} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedCategories.includes(category.name)}
                      onChange={() => handleCategoryToggle(category.name)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">{category.name}</span>
                  </label>
                ))}
              </div>
            )}
          </div>

          <div>
            <button
              onClick={() => toggleSection('tags')}
              className="flex items-center justify-between w-full text-sm font-medium text-gray-700 mb-3"
            >
              <span>태그</span>
              {expandedSections.tags ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
            {expandedSections.tags && (
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {topTags.map((tag) => (
                  <label key={tag.id} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(tag.name)}
                      onChange={() => handleTagToggle(tag.name)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">#{tag.name}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}