"""Build service dependency graph by analyzing imports and @Autowired fields."""
import re


def build_dependency_graph(java_files: list[dict]) -> dict:
    """Analyze Java source files to build a dependency graph of service interactions."""
    nodes = []
    edges = []

    # Build a map of class names to their types
    class_types: dict[str, str] = {}
    for jf in java_files:
        name = jf["filename"].replace(".java", "")
        if "Controller" in name:
            class_types[name] = "controller"
        elif "Service" in name:
            class_types[name] = "service"
        elif "Repository" in name:
            class_types[name] = "repository"
        elif jf["content"] and "@Entity" in jf["content"]:
            class_types[name] = "entity"
        else:
            class_types[name] = "other"

    # Create nodes
    for class_name, class_type in class_types.items():
        nodes.append({
            "id": class_name,
            "type": class_type,
            "label": class_name,
        })

    # Find dependencies via @Autowired fields
    autowired_pattern = re.compile(r'@Autowired\s+private\s+(\w+)\s+')

    for jf in java_files:
        source_class = jf["filename"].replace(".java", "")
        content = jf["content"]

        # @Autowired field injection
        for match in autowired_pattern.finditer(content):
            target_type = match.group(1)
            if target_type in class_types:
                edges.append({
                    "source": source_class,
                    "target": target_type,
                    "type": "field_injection",
                })

        # Direct method calls to other services (heuristic: look for service method calls)
        for other_class in class_types:
            if other_class == source_class:
                continue
            # Look for variable references like `loanService.someMethod(`
            var_name = other_class[0].lower() + other_class[1:]
            call_pattern = re.compile(rf'{re.escape(var_name)}\.\w+\s*\(')
            calls = call_pattern.findall(content)
            if calls:
                # Check if edge already exists
                existing = any(
                    e["source"] == source_class and e["target"] == other_class
                    for e in edges
                )
                if not existing:
                    edges.append({
                        "source": source_class,
                        "target": other_class,
                        "type": "method_call",
                        "call_count": len(calls),
                    })

    # Add cross-domain edges based on repository usage across services
    for jf in java_files:
        source_class = jf["filename"].replace(".java", "")
        if "Service" not in source_class:
            continue

        content = jf["content"]
        for repo_name, repo_type in class_types.items():
            if repo_type != "repository":
                continue
            var_name = repo_name[0].lower() + repo_name[1:]
            if var_name in content and repo_name != source_class:
                existing = any(
                    e["source"] == source_class and e["target"] == repo_name
                    for e in edges
                )
                if not existing:
                    edges.append({
                        "source": source_class,
                        "target": repo_name,
                        "type": "repository_access",
                    })

    return {
        "nodes": nodes,
        "edges": edges,
        "node_count": len(nodes),
        "edge_count": len(edges),
    }
