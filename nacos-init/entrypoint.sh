#!/bin/bash

# ========================================
# Nacos Server with Configuration Initializer
# ========================================

set -e

echo "========================================"
echo "  Nacos Server Initialization          "
echo "========================================"
echo "Start time: $(date)"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"; }

# 环境变量配置
NAMESPACE_ID="${NAMESPACE_ID:-dev}"
# 使用标准目录结构
CONFIG_DIR="/home/nacos/init.d/configs/${NAMESPACE_ID}"
# 备选目录
ALTERNATE_CONFIG_DIR="/home/nacos/init-configs/${NAMESPACE_ID}"

# 选择配置目录
select_config_dir() {
    if [ -d "$CONFIG_DIR" ] && [ "$(ls -A $CONFIG_DIR 2>/dev/null)" ]; then
        echo "$CONFIG_DIR"
        return 0
    elif [ -d "$ALTERNATE_CONFIG_DIR" ] && [ "$(ls -A $ALTERNATE_CONFIG_DIR 2>/dev/null)" ]; then
        echo "$ALTERNATE_CONFIG_DIR"
        return 0
    else
        log_warning "No configuration directory found at:"
        log_warning "  - $CONFIG_DIR"
        log_warning "  - $ALTERNATE_CONFIG_DIR"
        return 1
    fi
}

# 更新后续函数中使用配置目录的部分
# 在 import_all_configs 函数中：
import_all_configs() {
    # 选择配置目录
    local selected_dir=$(select_config_dir)
    if [ $? -ne 0 ]; then
        log_warning "No configuration files to import"
        return 0
    fi
    
    log_info "Starting configuration import from ${selected_dir}..."
    
    # 统计信息
    local total_files=0
    local success_count=0
    local skip_count=0
    local fail_count=0
    
    # 首先导入 common.yaml
    if [ -f "${selected_dir}/common.yaml" ]; then
        ((total_files++))
        if import_config "common.yaml" "${selected_dir}/common.yaml"; then
            ((success_count++))
        else
            ((fail_count++))
        fi
    fi
    
    # 导入其他配置文件
    for config_file in "${selected_dir}"/*.yaml; do
        if [ -f "$config_file" ] && [ "$(basename "$config_file")" != "common.yaml" ]; then
            local filename=$(basename "$config_file")
            ((total_files++))
            
            if import_config "$filename" "$config_file"; then
                ((success_count++))
            else
                ((fail_count++))
            fi
        fi
    done
    
    # 输出统计
    echo ""
    log_info "===== Configuration Import Summary ====="
    log_info "Configuration directory: ${selected_dir}"
    log_info "Total configuration files found: ${total_files}"
    log_success "Successfully imported: ${success_count}"
    log_info "Skipped (already exists): ${skip_count}"
    
    if [ $fail_count -gt 0 ]; then
        log_error "Failed to import: ${fail_count}"
    else
        log_success "All configurations imported successfully!"
    fi
}