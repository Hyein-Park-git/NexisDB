Name:           nexis-db-setup
Version:        1.0
Release:        1
Summary:        Nexis DB Setup - Database Initialization Tool
License:        Proprietary
BuildArch:      noarch

%description
Nexis DB Setup initializes the database schema required for Nexis Monitoring Platform.

%install
mkdir -p %{buildroot}/opt/nexis-db-setup

install -m 0755 %{_sourcedir}/nexis-db-setup.jar %{buildroot}/opt/nexis-db-setup/nexis-db-setup.jar

%files
%dir /opt/nexis-db-setup
/opt/nexis-db-setup/nexis-db-setup.jar

%pre
echo "[nexis-db-setup] Pre-install starting..."
if ! command -v java &> /dev/null; then
    echo "[nexis-db-setup] ERROR: Java is not installed. Please install Java 21 or higher."
    exit 1
fi
echo "[nexis-db-setup] Java check passed."

%post
CONFIGURED=0

cancel_install() {
    stty echo 2>/dev/null
    echo ""
    echo "[nexis-db-setup] Installation cancelled."
    exit 0
}

trap cancel_install INT TERM

if [ -e /dev/tty ]; then
    exec < /dev/tty

    echo "========================================"
    echo "  Nexis DB Setup Configuration"
    echo "  (Press Ctrl+C to cancel at any time)"
    echo "========================================"

    echo ""
    echo "Select DB type:"
    echo "  1) MySQL"
    echo "  2) MariaDB"
    echo "  3) PostgreSQL"
    printf "Enter choice [1-3] (default: 1): "
    read DB_TYPE_NUM < /dev/tty || cancel_install
    case $DB_TYPE_NUM in
        2) DB_TYPE="MariaDB"    ; DEFAULT_PORT="3306" ;;
        3) DB_TYPE="PostgreSQL" ; DEFAULT_PORT="5432" ;;
        *) DB_TYPE="MySQL"      ; DEFAULT_PORT="3306" ;;
    esac

    printf "DB Host [127.0.0.1]: "
    read DB_HOST < /dev/tty || cancel_install
    DB_HOST=${DB_HOST:-127.0.0.1}

    printf "DB Port [$DEFAULT_PORT]: "
    read DB_PORT < /dev/tty || cancel_install
    DB_PORT=${DB_PORT:-$DEFAULT_PORT}

    printf "DB Name [nexis]: "
    read DB_NAME < /dev/tty || cancel_install
    DB_NAME=${DB_NAME:-nexis}

    printf "DB Username: "
    read DB_USER < /dev/tty || cancel_install
    if [ -z "$DB_USER" ]; then
        echo "[nexis-db-setup] WARNING: DB Username is empty."
    fi

    printf "DB Password: "
    stty -echo 2>/dev/null
    read DB_PASS < /dev/tty || { stty echo 2>/dev/null; cancel_install; }
    stty echo 2>/dev/null
    echo ""

    echo ""
    echo "========================================"
    echo "  Configuration Summary"
    echo "========================================"
    echo "  DB Type : $DB_TYPE"
    echo "  DB Host : $DB_HOST"
    echo "  DB Port : $DB_PORT"
    echo "  DB Name : $DB_NAME"
    echo "  DB User : $DB_USER"
    echo "  DB Pass : ********"
    echo "========================================"
    printf "Apply this configuration? [Y/n]: "
    read CONFIRM < /dev/tty || cancel_install
    if [ "$CONFIRM" = "n" ] || [ "$CONFIRM" = "N" ]; then
        cancel_install
    fi

    echo ""
    echo "[nexis-db-setup] Initializing database..."
    java -jar /opt/nexis-db-setup/nexis-db-setup.jar \
        --type=$DB_TYPE \
        --host=$DB_HOST \
        --port=$DB_PORT \
        --name=$DB_NAME \
        --user=$DB_USER \
        --pass=$DB_PASS

    if [ $? -eq 0 ]; then
        echo "[nexis-db-setup] Database initialized successfully."
        CONFIGURED=1
    else
        echo "[nexis-db-setup] ERROR: Database initialization failed."
        echo "[nexis-db-setup] Please check your DB settings and try again."
        exit 1
    fi

else
    echo "[nexis-db-setup] No interactive terminal detected."
    echo "[nexis-db-setup] Run manually: java -jar /opt/nexis-db-setup/nexis-db-setup.jar --type=MySQL --host=127.0.0.1 --port=3306 --name=nexis --user=nexis --pass=password"
    CONFIGURED=1
fi

trap - INT TERM

if [ $CONFIGURED -eq 1 ]; then
    echo ""
    echo "========================================"
    echo "  Nexis DB Setup complete."
    echo "  You can now install Nexis Web and Nexis Server."
    echo "========================================"
fi

%preun
# nothing

%postun
# nothing

%changelog
* Thu Jan 01 2026 Nexis <nexis@example.com> - 1.0-1
- Initial release