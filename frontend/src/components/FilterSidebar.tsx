'use client';

import { Blog } from '@/types';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { useState } from 'react';

interface FilterSidebarProps {
  blogs: Blog[];
  tags: string[];
  categories: string[];
  selectedBlogs: number[];
  selectedTags: string[];
  selectedCategories: string[];
  onBlogChange: (blogIds: number[]) => void;
  onTagChange: (tags: string[]) => void;
  onCategoryChange: (categories: string[]) => void;
}

export default function FilterSidebar({
  blogs,
  tags,
  categories,
  selectedBlogs,
  selectedTags,
  selectedCategories,
  onBlogChange,
  onTagChange,
  onCategoryChange,
}: FilterSidebarProps) {
  const [expandedSections, setExpandedSections] = useState({
    blogs: true,
    categories: true,
    tags: false,
  });

  const toggleSection = (section: keyof typeof expandedSections) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  const handleBlogToggle = (blogId: number) => {
    if (selectedBlogs.includes(blogId)) {
      onBlogChange(selectedBlogs.filter(id => id !== blogId));
    } else {
      onBlogChange([...selectedBlogs, blogId]);
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

  const topTags = tags?.slice(0, 20) || [];

  return (
    <div className="w-64 bg-white shadow-sm border-r h-screen overflow-y-auto">
      <div className="p-4">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">필터</h2>

        <div className="space-y-6">
          <div>
            <button
              onClick={() => toggleSection('blogs')}
              className="flex items-center justify-between w-full text-sm font-medium text-gray-700 mb-3"
            >
              <span>블로그</span>
              {expandedSections.blogs ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
            {expandedSections.blogs && (
              <div className="space-y-2">
                {blogs.map((blog) => (
                  <label key={blog.id} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedBlogs.includes(blog.id)}
                      onChange={() => handleBlogToggle(blog.id)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">{blog.name}</span>
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
                  <label key={category} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedCategories.includes(category)}
                      onChange={() => handleCategoryToggle(category)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">{category}</span>
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
                  <label key={tag} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(tag)}
                      onChange={() => handleTagToggle(tag)}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">#{tag}</span>
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