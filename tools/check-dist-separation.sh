#!/usr/bin/env bash
#
# Fails if non-client code references client-only classes.
#
# A dedicated server never loads net.minecraft.client. A stray reference from common code is
# the classic crash-on-server-only bug, and it is invisible in single-player because the
# integrated server shares a JVM with a client.
#
# This exists because an earlier ad-hoc version of this check silently passed: javap was not on
# PATH, every invocation failed into /dev/null, and empty output was read as "no problems
# found". A check that cannot tell "nothing wrong" from "nothing ran" is worse than no check,
# so this one verifies its own tools first and errors out loudly if anything is missing.

set -euo pipefail

JAVA_HOME_DEFAULT="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"
JAVAP="${JAVA_HOME:-$JAVA_HOME_DEFAULT}/bin/javap"
[ -x "$JAVAP" ] || JAVAP="$JAVA_HOME_DEFAULT/bin/javap"

if ! "$JAVAP" -version >/dev/null 2>&1; then
    echo "FATAL: javap not usable at '$JAVAP'. Cannot verify; refusing to report success." >&2
    exit 2
fi

CLASSES_ROOT="${1:-}"
if [ -z "$CLASSES_ROOT" ] || [ ! -d "$CLASSES_ROOT" ]; then
    echo "usage: $0 <compiled-classes-dir>" >&2
    exit 2
fi

mapfile -t candidates < <(find "$CLASSES_ROOT" -name '*.class' | grep -v '/client/' | sort)
if [ "${#candidates[@]}" -eq 0 ]; then
    echo "FATAL: no non-client classes found under '$CLASSES_ROOT'. Did the build run?" >&2
    exit 2
fi

echo "Checking ${#candidates[@]} non-client class(es) under $CLASSES_ROOT"

violations=0
for f in "${candidates[@]}"; do
    # -v, not -c: a method reference (Foo::bar) compiles to an invokedynamic whose target class
    # appears only in the constant pool and BootstrapMethods table, which -c never prints. The
    # constant pool lists every class the file can touch, so it misses nothing -c would see.
    refs="$("$JAVAP" -p -v "$f" \
        | grep -oE 'net/minecraft/client/[A-Za-z0-9/$]*|com/ascension/[a-z_]+/client/[A-Za-z0-9/$]*' \
        | sort -u || true)"
    [ -z "$refs" ] && continue

    rel="${f#"$CLASSES_ROOT"/}"

    # Allowed: classes that name client code only inside an `if (FMLEnvironment.dist ==
    # Dist.CLIENT)` branch, which a dedicated server never executes (a method handle in the
    # constant pool is resolved only when its invokedynamic runs). Each was read and checked
    # by hand; add a class here only after doing the same.
    allowed=0
    for ok in \
        "com/ascension/atmosphere/internal/net/AtmosphereNetwork.class" \
        "com/ascension/worlds/internal/net/WorldsNetwork.class" \
        "com/ascension/combat/internal/net/CombatNetwork.class" \
        "com/ascension/combat/AscensionCombat.class"; do
        [ "$rel" == "$ok" ] && allowed=1
    done
    if [ "$allowed" -eq 1 ]; then
        echo "  ALLOWED  $rel (dist-guarded client reference)"
        continue
    fi

    # Allowed: the whole mod is client-only (`@Mod(dist = Dist.CLIENT)`, every dependency
    # `side = "CLIENT"`), so a dedicated server never loads any of its classes. Add a jar here
    # only after checking both of those in it.
    client_only=0
    for mod in immersivedamageindicators distanthorizons; do
        [[ "$rel" == com/ascension/compat/$mod/* ]] && client_only=1
    done
    if [ "$client_only" -eq 1 ]; then
        echo "  ALLOWED  $rel (client-only mod)"
        continue
    fi

    echo "  VIOLATION $rel"
    echo "$refs" | sed 's/^/              /'
    violations=$((violations + 1))
done

if [ "$violations" -gt 0 ]; then
    echo "FAILED: $violations class(es) reference client code from common code." >&2
    exit 1
fi

echo "OK: no unexpected client references in common code."
