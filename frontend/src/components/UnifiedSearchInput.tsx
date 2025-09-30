'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { Search, X } from 'lucide-react';
import { useTagSearch } from '@/hooks/useFilters';
import TagChip from './TagChip';

interface UnifiedSearchInputProps {
  value: string;
  onValueChange: (value: string) => void;
  selectedTags: string[];
  onTagsChange: (tags: string[]) => void;
  onSearch?: (query: string) => void;
  onClear?: () => void;
  placeholder?: string;
}

export default function UnifiedSearchInput({
  value,
  onValueChange,
  selectedTags,
  onTagsChange,
  onSearch,
  onClear,
  placeholder = "검색어 입력 또는 #으로 태그 검색..."
}: UnifiedSearchInputProps) {
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  const [tagQuery, setTagQuery] = useState('');
  const [isTagMode, setIsTagMode] = useState(false);
  const [tagModeStartPos, setTagModeStartPos] = useState(0);
  const [focusedTagIndex, setFocusedTagIndex] = useState(-1);
  const [focusedSuggestionIndex, setFocusedSuggestionIndex] = useState(-1);
  const [windowWidth, setWindowWidth] = useState(1024); // 기본값 설정
  
  // 프론트엔드 필터링으로 태그 검색
  const tagSuggestions = useTagSearch(tagQuery);
  
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);
  const tagChipRefs = useRef<(HTMLDivElement | null)[]>([]);
  const suggestionRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // 클라이언트 사이드에서 window width 설정
  useEffect(() => {
    const updateWindowWidth = () => {
      setWindowWidth(window.innerWidth);
    };

    // 초기 설정
    updateWindowWidth();

    // 리사이즈 이벤트 리스너 추가
    window.addEventListener('resize', updateWindowWidth);

    return () => {
      window.removeEventListener('resize', updateWindowWidth);
    };
  }, []);

  // 이미 선택된 태그를 제외한 제안 목록
  const filteredTagSuggestions = useCallback(() => {
    return tagSuggestions.filter(tag => !selectedTags.includes(tag.name));
  }, [tagSuggestions, selectedTags])();

  // 입력값 변경 처리 (자동 검색 제거)
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    const cursorPos = e.target.selectionStart || 0;
    
    onValueChange(newValue);

    // # 문자 감지
    const hashIndex = newValue.lastIndexOf('#', cursorPos - 1);
    if (hashIndex !== -1 && (hashIndex === 0 || /\s/.test(newValue[hashIndex - 1]))) {
      const tagStart = hashIndex + 1;
      const tagEnd = newValue.indexOf(' ', tagStart);
      const tagQuery = newValue.substring(tagStart, tagEnd === -1 ? newValue.length : tagEnd);
      
      setIsTagMode(true);
      setTagModeStartPos(hashIndex);
      setTagQuery(tagQuery);
      setShowTagSuggestions(true);
      // 첫 번째 제안 항목을 자동으로 선택
      setFocusedSuggestionIndex(0);
    } else {
      setIsTagMode(false);
      setShowTagSuggestions(false);
      setTagQuery('');
    }
  };

  // 태그 선택 처리
  const handleTagSelect = (tag: { name: string }) => {
    if (!selectedTags.includes(tag.name)) {
      onTagsChange([...selectedTags, tag.name]);
    }
    
    // 입력창에서 # 부분 제거
    if (isTagMode) {
      const beforeTag = value.substring(0, tagModeStartPos);
      const afterTag = value.substring(tagModeStartPos + tagQuery.length + 1);
      onValueChange((beforeTag + afterTag).trim());
    }
    
    setShowTagSuggestions(false);
    setIsTagMode(false);
    setFocusedSuggestionIndex(-1);
    inputRef.current?.focus();
  };

  // 태그 제거 처리
  const handleTagRemove = (tagToRemove: string) => {
    onTagsChange(selectedTags.filter(tag => tag !== tagToRemove));
  };

  // 키보드 처리 (Enter로 검색, ESC로 제안 닫기, 방향키로 태그 네비게이션, 백스페이스로 태그 삭제)
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      setShowTagSuggestions(false);
      setIsTagMode(false);
      setFocusedTagIndex(-1);
      setFocusedSuggestionIndex(-1);
      inputRef.current?.focus();
    } else if (showTagSuggestions && filteredTagSuggestions.length > 0) {
      // 태그 제안이 열려있을 때의 키보드 처리
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        e.stopPropagation();
        const newIndex = focusedSuggestionIndex < filteredTagSuggestions.length - 1 
          ? focusedSuggestionIndex + 1 
          : 0;
        setFocusedSuggestionIndex(newIndex);
        // 포커스 대신 스크롤로 보이게 하기
        setTimeout(() => {
          suggestionRefs.current[newIndex]?.scrollIntoView({
            block: 'nearest',
            behavior: 'smooth'
          });
        }, 0);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        e.stopPropagation();
        const newIndex = focusedSuggestionIndex > 0 
          ? focusedSuggestionIndex - 1 
          : filteredTagSuggestions.length - 1;
        setFocusedSuggestionIndex(newIndex);
        // 포커스 대신 스크롤로 보이게 하기
        setTimeout(() => {
          suggestionRefs.current[newIndex]?.scrollIntoView({
            block: 'nearest',
            behavior: 'smooth'
          });
        }, 0);
      } else if (e.key === 'Enter' && focusedSuggestionIndex >= 0) {
        e.preventDefault();
        e.stopPropagation();
        const selectedTag = filteredTagSuggestions[focusedSuggestionIndex];
        handleTagSelect(selectedTag);
      }
    } else if (e.key === 'Enter' && !showTagSuggestions && onSearch) {
      e.preventDefault();
      onSearch(value);
    } else if (e.key === 'ArrowLeft' && selectedTags.length > 0 && !showTagSuggestions) {
      e.preventDefault();
      if (focusedTagIndex === -1) {
        // 입력창에서 첫 번째 태그로 이동
        setFocusedTagIndex(selectedTags.length - 1);
        tagChipRefs.current[selectedTags.length - 1]?.focus();
      } else if (focusedTagIndex > 0) {
        // 이전 태그로 이동
        const newIndex = focusedTagIndex - 1;
        setFocusedTagIndex(newIndex);
        tagChipRefs.current[newIndex]?.focus();
      }
    } else if (e.key === 'ArrowRight' && selectedTags.length > 0 && !showTagSuggestions) {
      e.preventDefault();
      if (focusedTagIndex >= 0 && focusedTagIndex < selectedTags.length - 1) {
        // 다음 태그로 이동
        const newIndex = focusedTagIndex + 1;
        setFocusedTagIndex(newIndex);
        tagChipRefs.current[newIndex]?.focus();
      } else if (focusedTagIndex === selectedTags.length - 1) {
        // 마지막 태그에서 입력창으로 이동
        setFocusedTagIndex(-1);
        inputRef.current?.focus();
      }
    } else if (e.key === 'Backspace' && value === '' && selectedTags.length > 0 && !showTagSuggestions) {
      e.preventDefault();
      if (focusedTagIndex === -1) {
        // 입력창이 비어있을 때 마지막 태그 삭제
        const lastTag = selectedTags[selectedTags.length - 1];
        handleTagRemove(lastTag);
      } else {
        // 포커스된 태그 삭제
        const tagToRemove = selectedTags[focusedTagIndex];
        handleTagRemove(tagToRemove);
        // 포커스 인덱스 조정
        if (focusedTagIndex >= selectedTags.length - 1) {
          setFocusedTagIndex(Math.max(0, selectedTags.length - 2));
        }
      }
    }
  };

  // 검색 버튼 클릭
  const handleSearchClick = () => {
    if (onSearch) {
      onSearch(value);
    }
  };

  // 외부 클릭 시 제안 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node) &&
        !inputRef.current?.contains(event.target as Node)
      ) {
        setShowTagSuggestions(false);
      }
    };

    if (showTagSuggestions) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showTagSuggestions]);

  // 전체 초기화
  const handleClear = () => {
    // 먼저 완전한 초기화를 실행
    if (onClear) {
      onClear();
    }
    
    // UI 상태 정리
    setShowTagSuggestions(false);
    setIsTagMode(false);
    inputRef.current?.focus();
  };

  const hasContent = value.trim() || selectedTags.length > 0;

  // 칩들의 너비를 계산하여 동적 padding 설정
  const chipsRef = useRef<HTMLDivElement>(null);
  const [chipsWidth, setChipsWidth] = useState(0);

  useEffect(() => {
    if (chipsRef.current && selectedTags.length > 0) {
      setChipsWidth(chipsRef.current.scrollWidth + 8); // 8px for margin
    } else {
      setChipsWidth(0);
    }
  }, [selectedTags]);

  // 태그 배열이 변경될 때 ref 배열 크기 조정
  useEffect(() => {
    tagChipRefs.current = tagChipRefs.current.slice(0, selectedTags.length);
  }, [selectedTags.length]);

  // 태그가 제거될 때 포커스 인덱스 조정
  useEffect(() => {
    if (focusedTagIndex >= selectedTags.length) {
      setFocusedTagIndex(Math.max(-1, selectedTags.length - 1));
    }
  }, [selectedTags.length, focusedTagIndex]);

  // 태그 제안 배열이 변경될 때 ref 배열 크기 조정
  useEffect(() => {
    suggestionRefs.current = suggestionRefs.current.slice(0, filteredTagSuggestions.length);
  }, [filteredTagSuggestions.length]);

  // 태그 제안이 변경될 때 포커스 인덱스 조정
  useEffect(() => {
    if (showTagSuggestions && filteredTagSuggestions.length > 0) {
      // 태그 제안이 열려있고 항목이 있으면 첫 번째 항목 선택
      if (focusedSuggestionIndex === -1 || focusedSuggestionIndex >= filteredTagSuggestions.length) {
        setFocusedSuggestionIndex(0);
      }
    } else {
      setFocusedSuggestionIndex(-1);
    }
  }, [filteredTagSuggestions.length, showTagSuggestions, focusedSuggestionIndex]);

  return (
    <div className="relative w-full">
      {/* 검색 입력창 */}
      <div className="relative">
        {/* 왜쪽 아이콘 */}
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none z-10">
          <Search className="h-4 w-4 text-slate-400" />
        </div>
        
        {/* 칩들이 입력창 내부에 위치 */}
        {selectedTags.length > 0 && (
          <div 
            ref={chipsRef}
            className="absolute inset-y-0 left-10 flex items-center gap-1 max-w-[60%] overflow-x-auto overflow-y-hidden z-10 py-2 pr-2"
            style={{ 
              scrollbarWidth: 'none', 
              msOverflowStyle: 'none',
            }}
          >
            <style jsx>{`
              div::-webkit-scrollbar {
                display: none;
              }
            `}</style>
            {selectedTags.map((tag, index) => (
              <TagChip
                key={tag}
                ref={(el) => {
                  tagChipRefs.current[index] = el;
                }}
                tag={tag}
                onRemove={() => handleTagRemove(tag)}
                compact={true}
                isFocused={focusedTagIndex === index}
                onFocus={() => setFocusedTagIndex(index)}
                onKeyDown={(e) => {
                  if (e.key === 'ArrowLeft' && index > 0) {
                    e.preventDefault();
                    setFocusedTagIndex(index - 1);
                    tagChipRefs.current[index - 1]?.focus();
                  } else if (e.key === 'ArrowRight' && index < selectedTags.length - 1) {
                    e.preventDefault();
                    setFocusedTagIndex(index + 1);
                    tagChipRefs.current[index + 1]?.focus();
                  } else if (e.key === 'ArrowRight' && index === selectedTags.length - 1) {
                    e.preventDefault();
                    setFocusedTagIndex(-1);
                    inputRef.current?.focus();
                  }
                }}
              />
            ))}
          </div>
        )}
        
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onFocus={() => setFocusedTagIndex(-1)}
          placeholder={selectedTags.length > 0 ? "" : placeholder}
          className={`w-full py-3 text-slate-800 placeholder:text-slate-500 border border-slate-200 rounded-xl
                   focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400
                   transition-all duration-200`}
          style={{
            paddingLeft: selectedTags.length > 0 ? `${Math.min(chipsWidth + 48, windowWidth * 0.6)}px` : '40px',
            paddingRight: onSearch ? '80px' : '40px'
          }}
        />
        
        <div className="absolute inset-y-0 right-0 flex items-center">
          {onSearch && (
            <button
              onClick={handleSearchClick}
              className="px-3 py-2 mr-1 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
              type="button"
              title="검색"
            >
              <Search className="h-4 w-4" />
            </button>
          )}
          {hasContent && (
            <button
              onClick={handleClear}
              className="pr-3 py-2 text-slate-400 hover:text-slate-600 transition-colors"
              type="button"
              title="지우기"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {/* 태그 제안 드롭다운 */}
      {showTagSuggestions && (
        <div
          ref={suggestionsRef}
          className="absolute top-full left-0 right-0 mt-2 bg-white border border-slate-200 rounded-xl shadow-xl z-50 max-h-60 overflow-y-auto"
        >
          {filteredTagSuggestions.length > 0 ? (
            <div className="p-2">
              {filteredTagSuggestions.map((tag, index) => (
                <button
                  key={tag.id && tag.id !== 0 ? tag.id : `tag-${index}-${tag.name}`}
                  ref={(el) => {
                    suggestionRefs.current[index] = el;
                  }}
                  onClick={() => handleTagSelect(tag)}
                  className={`w-full text-left px-3 py-2 rounded-lg transition-colors flex items-center space-x-2 ${
                    focusedSuggestionIndex === index
                      ? 'bg-blue-50 text-blue-800 ring-1 ring-blue-200'
                      : 'hover:bg-slate-50'
                  }`}
                  type="button"
                >
                  <span className="text-slate-600 font-mono">#</span>
                  <span className="text-slate-800">{tag.name}</span>
                  {tag.description && (
                    <span className="text-xs text-slate-500 ml-auto">
                      {tag.description}
                    </span>
                  )}
                </button>
              ))}
            </div>
          ) : (
            <div className="p-4 text-center text-slate-500 text-sm">
              {tagQuery ? '검색 결과가 없습니다' : '태그를 입력하세요'}
            </div>
          )}
        </div>
      )}
    </div>
  );
}