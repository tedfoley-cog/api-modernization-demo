"""Main analysis script — parses Java source, runs all analyzers, outputs JSON for dashboard."""
import json
import os
import re
import sys

from analysis.coupling_scorer import calculate_coupling_scores
from analysis.dependency_graph import build_dependency_graph
from analysis.domain_boundary import identify_domain_boundaries
from analysis.event_catalog import generate_event_catalog


def find_java_files(source_dir: str) -> list[dict]:
    """Find all Java source files and extract basic metadata."""
    java_files = []
    for root, _dirs, files in os.walk(source_dir):
        for f in files:
            if f.endswith(".java"):
                filepath = os.path.join(root, f)
                rel_path = os.path.relpath(filepath, source_dir)
                with open(filepath, encoding="utf-8") as fh:
                    content = fh.read()
                java_files.append({
                    "filename": f,
                    "path": rel_path,
                    "full_path": filepath,
                    "content": content,
                    "line_count": len(content.splitlines()),
                })
    return java_files


def extract_endpoints(java_files: list[dict]) -> list[dict]:
    """Extract REST API endpoints from Spring MVC controller files."""
    endpoints = []
    request_mapping_pattern = re.compile(
        r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\'])?\s*\)?'
    )
    class_mapping_pattern = re.compile(
        r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']'
    )

    for jf in java_files:
        if "Controller" not in jf["filename"]:
            continue

        content = jf["content"]
        class_path = ""
        class_match = class_mapping_pattern.search(content)
        if class_match:
            class_path = class_match.group(1)

        for match in request_mapping_pattern.finditer(content):
            method = match.group(1).upper()
            path = match.group(2) if match.group(2) else ""
            full_path = class_path + ("/" + path if path else "")
            full_path = full_path.replace("//", "/")

            endpoints.append({
                "method": method,
                "path": full_path,
                "controller": jf["filename"].replace(".java", ""),
                "source_file": jf["path"],
            })

    return endpoints


def extract_service_methods(java_files: list[dict]) -> list[dict]:
    """Extract public methods from service classes."""
    services = []
    method_pattern = re.compile(
        r'public\s+(?:[\w<>,\s\[\]]+)\s+(\w+)\s*\(([^)]*)\)'
    )

    for jf in java_files:
        if "Service" not in jf["filename"] or jf["filename"].endswith("Test.java"):
            continue

        content = jf["content"]
        methods = []
        for match in method_pattern.finditer(content):
            method_name = match.group(1)
            params = match.group(2).strip()
            if method_name in ("main", "toString", "hashCode", "equals"):
                continue
            methods.append({
                "name": method_name,
                "parameters": params,
            })

        # Count lines of code
        loc = len(content.splitlines())

        # Count @Autowired fields (field injection smell)
        autowired_count = len(re.findall(r'@Autowired', content))

        services.append({
            "class_name": jf["filename"].replace(".java", ""),
            "source_file": jf["path"],
            "methods": methods,
            "method_count": len(methods),
            "line_count": loc,
            "autowired_dependencies": autowired_count,
        })

    return services


def extract_entities(java_files: list[dict]) -> list[dict]:
    """Extract JPA entity information."""
    entities = []
    table_pattern = re.compile(r'@Table\s*\(\s*name\s*=\s*["\']([^"\']*)["\']')
    column_pattern = re.compile(
        r'@Column\s*\([^)]*name\s*=\s*["\']([^"\']*)["\'][^)]*\)'
    )
    field_pattern = re.compile(r'private\s+([\w<>,\s]+?)\s+(\w+)\s*;')

    for jf in java_files:
        content = jf["content"]
        if "@Entity" not in content:
            continue

        table_match = table_pattern.search(content)
        table_name = table_match.group(1) if table_match else jf["filename"].replace(".java", "").lower() + "s"

        columns = column_pattern.findall(content)
        fields = field_pattern.findall(content)

        entities.append({
            "class_name": jf["filename"].replace(".java", ""),
            "table_name": table_name,
            "source_file": jf["path"],
            "column_count": len(columns),
            "field_count": len(fields),
            "fields": [{"type": f[0].strip(), "name": f[1]} for f in fields],
        })

    return entities


def compute_metrics(java_files: list[dict], services: list[dict], endpoints: list[dict]) -> dict:
    """Compute codebase-level metrics."""
    total_loc = sum(jf["line_count"] for jf in java_files)
    service_loc = sum(s["line_count"] for s in services)
    avg_methods = (
        sum(s["method_count"] for s in services) / len(services) if services else 0
    )
    max_service = max(services, key=lambda s: s["line_count"]) if services else None

    return {
        "total_java_files": len(java_files),
        "total_lines_of_code": total_loc,
        "total_service_loc": service_loc,
        "total_endpoints": len(endpoints),
        "total_services": len(services),
        "avg_methods_per_service": round(avg_methods, 1),
        "largest_service": {
            "name": max_service["class_name"] if max_service else "N/A",
            "loc": max_service["line_count"] if max_service else 0,
        },
        "total_autowired_injections": sum(
            s["autowired_dependencies"] for s in services
        ),
    }


def main():
    source_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    output_dir = sys.argv[2] if len(sys.argv) > 2 else "dashboard/data"

    print(f"Analyzing Java source in: {source_dir}")
    print(f"Output directory: {output_dir}")

    os.makedirs(output_dir, exist_ok=True)

    # Step 1: Find and parse Java files
    java_files = find_java_files(source_dir)
    print(f"Found {len(java_files)} Java files")

    # Step 2: Extract endpoints
    endpoints = extract_endpoints(java_files)
    print(f"Extracted {len(endpoints)} API endpoints")

    # Step 3: Extract service information
    services = extract_service_methods(java_files)
    print(f"Analyzed {len(services)} service classes")

    # Step 4: Extract entity information
    entities = extract_entities(java_files)
    print(f"Found {len(entities)} JPA entities")

    # Step 5: Build dependency graph
    dep_graph = build_dependency_graph(java_files)
    print(f"Built dependency graph with {len(dep_graph['edges'])} edges")

    # Step 6: Calculate coupling scores
    coupling = calculate_coupling_scores(java_files, services, dep_graph)
    print(f"Calculated coupling scores for {len(coupling)} services")

    # Step 7: Identify domain boundaries
    boundaries = identify_domain_boundaries(endpoints, services, entities, dep_graph)
    print(f"Identified {len(boundaries)} domain boundaries")

    # Step 8: Generate event catalog
    events = generate_event_catalog(services, boundaries)
    print(f"Generated {len(events)} domain events")

    # Step 9: Compute metrics
    metrics = compute_metrics(java_files, services, endpoints)
    print(f"Total LOC: {metrics['total_lines_of_code']}")

    # Write output JSON files
    analysis_output = {
        "endpoints": endpoints,
        "services": services,
        "entities": entities,
        "dependency_graph": dep_graph,
        "coupling_scores": coupling,
        "domain_boundaries": boundaries,
        "event_catalog": events,
        "metrics": metrics,
    }

    with open(os.path.join(output_dir, "analysis.json"), "w") as f:
        json.dump(analysis_output, f, indent=2, default=str)
    print(f"\nAnalysis complete. Output written to {output_dir}/analysis.json")

    # Write individual files for dashboard sections
    for key in ["endpoints", "services", "entities", "coupling_scores",
                 "domain_boundaries", "event_catalog", "metrics"]:
        with open(os.path.join(output_dir, f"{key}.json"), "w") as f:
            json.dump(analysis_output[key], f, indent=2, default=str)

    with open(os.path.join(output_dir, "dependency_graph.json"), "w") as f:
        json.dump(dep_graph, f, indent=2, default=str)

    print("Individual data files written.")


if __name__ == "__main__":
    main()
