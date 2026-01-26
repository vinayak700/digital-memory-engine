#!/bin/bash
# =============================================================================
#  Digital Memory Engine - Interactive Terminal UI
#  A beautiful, menu-driven interface to manage your memories
# =============================================================================

# Configuration
BASE_URL="http://localhost:8082"
API_URL="$BASE_URL/api/v1"
USER_ID="default-user"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color
BOLD='\033[1m'
DIM='\033[2m'

# Unicode symbols
CHECK="✓"
CROSS="✗"
ARROW="➤"
BRAIN="🧠"
SEARCH="🔍"
LINK="🔗"
STAR="★"
GEAR="⚙️"
BOOK="📚"

# Clear screen and show header
show_header() {
    clear
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║                                                                  ║"
    echo "║   ${WHITE}${BOLD}$BRAIN  DIGITAL MEMORY ENGINE${NC}${CYAN}                                  ║"
    echo "║   ${DIM}Your Personal Knowledge Management System${NC}${CYAN}                     ║"
    echo "║                                                                  ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo -e "${GRAY}Current User: ${WHITE}$USER_ID${NC}    ${GRAY}API: ${WHITE}$BASE_URL${NC}"
    echo ""
}

# Show a loading spinner
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " ${CYAN}%c${NC}  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# Divider
divider() {
    echo -e "${GRAY}─────────────────────────────────────────────────────────────────────${NC}"
}

# Success message
success() {
    echo -e "${GREEN}${CHECK} $1${NC}"
}

# Error message
error() {
    echo -e "${RED}${CROSS} $1${NC}"
}

# Info message
info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

# Prompt for input
prompt() {
    echo -ne "${YELLOW}${ARROW} $1: ${NC}"
}

# Wait for keypress
pause() {
    echo ""
    echo -ne "${GRAY}Press any key to continue...${NC}"
    read -n 1 -s
}

# Check if server is running
check_server() {
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
    if [ "$response" == "200" ]; then
        return 0
    else
        return 1
    fi
}

# ============================================================================
#  MEMORY FUNCTIONS
# ============================================================================

create_memory() {
    show_header
    echo -e "${MAGENTA}${BOOK} CREATE NEW MEMORY${NC}"
    divider
    echo ""
    
    prompt "Title"
    read title
    
    echo ""
    prompt "Content"
    read content
    
    echo ""
    prompt "Importance (1-10)"
    read importance
    
    if [ -z "$importance" ]; then
        importance=5
    fi
    
    echo ""
    info "Creating memory..."
    
    response=$(curl -s -X POST "$API_URL/memories" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: $USER_ID" \
        -d "{\"title\":\"$title\",\"content\":\"$content\",\"importanceScore\":$importance}")
    
    id=$(echo "$response" | grep -o '"id":[0-9]*' | cut -d: -f2)
    
    if [ -n "$id" ]; then
        echo ""
        success "Memory created successfully!"
        echo ""
        echo -e "  ${WHITE}ID:${NC} $id"
        echo -e "  ${WHITE}Title:${NC} $title"
        echo -e "  ${WHITE}Importance:${NC} $importance/10"
    else
        error "Failed to create memory"
        echo -e "${DIM}$response${NC}"
    fi
    
    pause
}

list_memories() {
    show_header
    echo -e "${MAGENTA}${BOOK} YOUR MEMORIES${NC}"
    divider
    echo ""
    
    info "Loading memories..."
    
    response=$(curl -s -X GET "$API_URL/memories?page=0&size=20" \
        -H "X-User-Id: $USER_ID")
    
    # Check if empty
    if [ "$response" == "[]" ]; then
        echo ""
        echo -e "${DIM}No memories found. Create one to get started!${NC}"
    else
        echo ""
        # Parse and display memories
        echo "$response" | grep -o '"id":[0-9]*,"title":"[^"]*"[^}]*"importanceScore":[0-9]*' | while read line; do
            id=$(echo "$line" | grep -o '"id":[0-9]*' | cut -d: -f2)
            title=$(echo "$line" | grep -o '"title":"[^"]*"' | cut -d: -f2 | tr -d '"')
            score=$(echo "$line" | grep -o '"importanceScore":[0-9]*' | cut -d: -f2)
            
            # Stars based on importance
            stars=""
            for ((i=0; i<score; i++)); do
                stars+="$STAR"
            done
            
            echo -e "  ${CYAN}[$id]${NC} ${WHITE}$title${NC}"
            echo -e "       ${YELLOW}$stars${NC}"
            echo ""
        done
    fi
    
    pause
}

view_memory() {
    show_header
    echo -e "${MAGENTA}${BOOK} VIEW MEMORY${NC}"
    divider
    echo ""
    
    prompt "Enter Memory ID"
    read memory_id
    
    if [ -z "$memory_id" ]; then
        error "Memory ID is required"
        pause
        return
    fi
    
    response=$(curl -s -X GET "$API_URL/memories/$memory_id" \
        -H "X-User-Id: $USER_ID")
    
    # Check for error
    if echo "$response" | grep -q "RESOURCE_NOT_FOUND"; then
        echo ""
        error "Memory not found"
    else
        echo ""
        title=$(echo "$response" | grep -o '"title":"[^"]*"' | cut -d: -f2 | tr -d '"')
        content=$(echo "$response" | grep -o '"content":"[^"]*"' | cut -d: -f2 | tr -d '"')
        score=$(echo "$response" | grep -o '"importanceScore":[0-9]*' | cut -d: -f2)
        created=$(echo "$response" | grep -o '"createdAt":"[^"]*"' | cut -d: -f2 | tr -d '"')
        
        echo -e "  ${WHITE}${BOLD}$title${NC}"
        divider
        echo ""
        echo -e "  ${content}"
        echo ""
        divider
        echo -e "  ${GRAY}Importance:${NC} ${YELLOW}$score/10${NC}"
        echo -e "  ${GRAY}Created:${NC} $created"
    fi
    
    pause
}

update_memory() {
    show_header
    echo -e "${MAGENTA}${GEAR} UPDATE MEMORY${NC}"
    divider
    echo ""
    
    prompt "Enter Memory ID"
    read memory_id
    
    if [ -z "$memory_id" ]; then
        error "Memory ID is required"
        pause
        return
    fi
    
    echo ""
    prompt "New Title (leave empty to skip)"
    read new_title
    
    prompt "New Importance 1-10 (leave empty to skip)"
    read new_importance
    
    # Build JSON
    json="{"
    if [ -n "$new_title" ]; then
        json+="\"title\":\"$new_title\""
    fi
    if [ -n "$new_importance" ]; then
        if [ -n "$new_title" ]; then json+=","; fi
        json+="\"importanceScore\":$new_importance"
    fi
    json+="}"
    
    if [ "$json" == "{}" ]; then
        error "Nothing to update"
        pause
        return
    fi
    
    response=$(curl -s -X PATCH "$API_URL/memories/$memory_id" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: $USER_ID" \
        -d "$json")
    
    if echo "$response" | grep -q '"id"'; then
        echo ""
        success "Memory updated successfully!"
    else
        error "Failed to update memory"
        echo -e "${DIM}$response${NC}"
    fi
    
    pause
}

archive_memory() {
    show_header
    echo -e "${MAGENTA}${CROSS} ARCHIVE MEMORY${NC}"
    divider
    echo ""
    
    prompt "Enter Memory ID to archive"
    read memory_id
    
    if [ -z "$memory_id" ]; then
        error "Memory ID is required"
        pause
        return
    fi
    
    echo ""
    echo -e "${YELLOW}Are you sure you want to archive memory #$memory_id? (y/n)${NC}"
    prompt "Confirm"
    read confirm
    
    if [ "$confirm" == "y" ] || [ "$confirm" == "Y" ]; then
        curl -s -X DELETE "$API_URL/memories/$memory_id" \
            -H "X-User-Id: $USER_ID" > /dev/null
        
        echo ""
        success "Memory archived"
    else
        info "Cancelled"
    fi
    
    pause
}

# ============================================================================
#  SEARCH FUNCTIONS
# ============================================================================

search_memories() {
    show_header
    echo -e "${MAGENTA}${SEARCH} SEARCH MEMORIES${NC}"
    divider
    echo ""
    
    prompt "Enter search query"
    read query
    
    if [ -z "$query" ]; then
        error "Query is required"
        pause
        return
    fi
    
    prompt "Max results (default: 5)"
    read limit
    
    if [ -z "$limit" ]; then
        limit=5
    fi
    
    echo ""
    info "Searching..."
    
    response=$(curl -s -X POST "$API_URL/search" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: $USER_ID" \
        -d "{\"query\":\"$query\",\"limit\":$limit}")
    
    if [ "$response" == "[]" ]; then
        echo ""
        echo -e "${DIM}No results found for: $query${NC}"
    else
        echo ""
        echo -e "${WHITE}Search Results:${NC}"
        divider
        echo "$response"
    fi
    
    pause
}

# ============================================================================
#  RELATIONSHIP FUNCTIONS
# ============================================================================

create_relationship() {
    show_header
    echo -e "${MAGENTA}${LINK} CREATE RELATIONSHIP${NC}"
    divider
    echo ""
    
    prompt "Source Memory ID"
    read source_id
    
    prompt "Target Memory ID"
    read target_id
    
    echo ""
    echo -e "  ${WHITE}Relationship Types:${NC}"
    echo -e "    ${CYAN}1${NC}) RELATED_TO"
    echo -e "    ${CYAN}2${NC}) CAUSED_BY"
    echo -e "    ${CYAN}3${NC}) FOLLOWED_BY"
    echo -e "    ${CYAN}4${NC}) SUPPORTS"
    echo -e "    ${CYAN}5${NC}) CONTRADICTS"
    echo ""
    
    prompt "Select type (1-5)"
    read type_choice
    
    case $type_choice in
        1) rel_type="RELATED_TO" ;;
        2) rel_type="CAUSED_BY" ;;
        3) rel_type="FOLLOWED_BY" ;;
        4) rel_type="SUPPORTS" ;;
        5) rel_type="CONTRADICTS" ;;
        *) rel_type="RELATED_TO" ;;
    esac
    
    prompt "Strength (0.0-1.0, default: 0.5)"
    read strength
    
    if [ -z "$strength" ]; then
        strength="0.5"
    fi
    
    response=$(curl -s -X POST "$API_URL/relationships" \
        -H "Content-Type: application/json" \
        -H "X-User-Id: $USER_ID" \
        -d "{\"sourceMemoryId\":$source_id,\"targetMemoryId\":$target_id,\"type\":\"$rel_type\",\"strength\":$strength}")
    
    echo ""
    success "Relationship created: #$source_id → #$target_id ($rel_type)"
    
    pause
}

view_relationships() {
    show_header
    echo -e "${MAGENTA}${LINK} VIEW RELATIONSHIPS${NC}"
    divider
    echo ""
    
    prompt "Enter Memory ID"
    read memory_id
    
    if [ -z "$memory_id" ]; then
        error "Memory ID is required"
        pause
        return
    fi
    
    response=$(curl -s -X GET "$API_URL/relationships/memory/$memory_id" \
        -H "X-User-Id: $USER_ID")
    
    echo ""
    if [ "$response" == "[]" ]; then
        echo -e "${DIM}No relationships found for memory #$memory_id${NC}"
    else
        echo -e "${WHITE}Related Memories:${NC}"
        divider
        echo "$response" | grep -o '"memoryId":[0-9]*,"title":"[^"]*"' | while read line; do
            id=$(echo "$line" | grep -o '"memoryId":[0-9]*' | cut -d: -f2)
            title=$(echo "$line" | grep -o '"title":"[^"]*"' | cut -d: -f2 | tr -d '"')
            echo -e "  ${CYAN}→${NC} [$id] $title"
        done
    fi
    
    pause
}

# ============================================================================
#  ASK INTELLIGENCE ENGINE
# ============================================================================

ask_question() {
    show_header
    echo -e "${MAGENTA}🤖 ASK INTELLIGENCE ENGINE${NC}"
    divider
    echo ""
    echo -e "  ${DIM}Ask a natural language question about your memories.${NC}"
    echo -e "  ${DIM}The engine will synthesize an answer from relevant memories.${NC}"
    echo ""
    
    prompt "Your question"
    read question
    
    if [ -z "$question" ]; then
        error "Question is required"
        pause
        return
    fi
    
    echo ""
    info "Thinking..."
    
    # URL encode the question for GET request
    encoded_question=$(echo "$question" | sed 's/ /+/g')
    
    response=$(curl -s -X GET "$API_URL/ask?q=$encoded_question" \
        -H "X-User-Id: $USER_ID")
    
    # Check for error
    if echo "$response" | grep -q '"answer"'; then
        answer=$(echo "$response" | grep -o '"answer":"[^"]*' | cut -d: -f2- | tr -d '"' | head -c 500)
        confidence=$(echo "$response" | grep -o '"confidence":[0-9.]*' | cut -d: -f2)
        
        echo ""
        echo -e "${WHITE}${BOLD}Answer:${NC}"
        divider
        echo ""
        echo -e "  $answer"
        echo ""
        divider
        echo -e "  ${GRAY}Confidence:${NC} ${YELLOW}${confidence}${NC}"
        
        # Show sources if available
        if echo "$response" | grep -q '"sources"'; then
            echo ""
            echo -e "  ${WHITE}Sources:${NC}"
            echo "$response" | grep -o '"title":"[^"]*"' | while read line; do
                title=$(echo "$line" | cut -d: -f2 | tr -d '"')
                echo -e "    ${CYAN}→${NC} $title"
            done
        fi
    else
        echo ""
        error "Failed to get answer"
        echo -e "${DIM}$response${NC}"
    fi
    
    pause
}

# ============================================================================
#  SETTINGS
# ============================================================================

change_user() {
    show_header
    echo -e "${MAGENTA}${GEAR} SETTINGS${NC}"
    divider
    echo ""
    
    echo -e "  ${WHITE}Current User:${NC} $USER_ID"
    echo ""
    
    prompt "Enter new User ID"
    read new_user
    
    if [ -n "$new_user" ]; then
        USER_ID="$new_user"
        success "User changed to: $USER_ID"
    else
        info "User unchanged"
    fi
    
    pause
}

# ============================================================================
#  MAIN MENU
# ============================================================================

show_main_menu() {
    show_header
    
    # Check server status
    if check_server; then
        echo -e "  ${GREEN}● Server Online${NC}"
    else
        echo -e "  ${RED}● Server Offline${NC}"
        echo -e "  ${DIM}Start with: ./mvnw spring-boot:run${NC}"
    fi
    
    echo ""
    echo -e "  ${WHITE}${BOLD}MAIN MENU${NC}"
    divider
    echo ""
    echo -e "  ${CYAN}${BOLD}MEMORIES${NC}"
    echo -e "    ${WHITE}1${NC}) ${BRAIN} Create Memory"
    echo -e "    ${WHITE}2${NC}) ${BOOK} List Memories"
    echo -e "    ${WHITE}3${NC}) ${SEARCH} View Memory"
    echo -e "    ${WHITE}4${NC}) ${GEAR} Update Memory"
    echo -e "    ${WHITE}5${NC}) ${CROSS} Archive Memory"
    echo ""
    echo -e "  ${CYAN}${BOLD}SEARCH${NC}"
    echo -e "    ${WHITE}6${NC}) ${SEARCH} Search Memories"
    echo ""
    echo -e "  ${CYAN}${BOLD}RELATIONSHIPS${NC}"
    echo -e "    ${WHITE}7${NC}) ${LINK} Create Relationship"
    echo -e "    ${WHITE}8${NC}) ${LINK} View Relationships"
    echo ""
    echo -e "  ${CYAN}${BOLD}INTELLIGENCE${NC}"
    echo -e "    ${WHITE}10${NC}) 🤖 Ask Question"
    echo ""
    echo -e "  ${CYAN}${BOLD}SETTINGS${NC}"
    echo -e "    ${WHITE}9${NC}) ${GEAR} Change User"
    echo ""
    echo -e "    ${WHITE}0${NC}) Exit"
    echo ""
    divider
    prompt "Select option"
}

# ============================================================================
#  MAIN LOOP
# ============================================================================

main() {
    while true; do
        show_main_menu
        read choice
        
        case $choice in
            1) create_memory ;;
            2) list_memories ;;
            3) view_memory ;;
            4) update_memory ;;
            5) archive_memory ;;
            6) search_memories ;;
            7) create_relationship ;;
            8) view_relationships ;;
            9) change_user ;;
            10) ask_question ;;
            0) 
                clear
                echo -e "${CYAN}Goodbye! 👋${NC}"
                exit 0
                ;;
            *)
                error "Invalid option"
                sleep 1
                ;;
        esac
    done
}

# Run the app
main
