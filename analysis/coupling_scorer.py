"""Calculate coupling scores for each service based on cross-domain dependencies."""


def calculate_coupling_scores(
    java_files: list[dict],
    services: list[dict],
    dep_graph: dict,
) -> list[dict]:
    """Score each service by how tightly coupled it is to other domains."""

    # Map service names to their domain based on naming convention
    domain_map: dict[str, str] = {}
    for s in services:
        name = s["class_name"]
        if "Loan" in name:
            domain_map[name] = "loan-origination"
        elif "Payment" in name:
            domain_map[name] = "payment-processing"
        elif "Account" in name:
            domain_map[name] = "account-servicing"
        elif "Dealer" in name:
            domain_map[name] = "dealer-integration"
        elif "Report" in name:
            domain_map[name] = "reporting"
        else:
            domain_map[name] = "shared"

    # Also map repositories to domains
    for node in dep_graph.get("nodes", []):
        name = node["id"]
        if node["type"] == "repository":
            if "Loan" in name:
                domain_map[name] = "loan-origination"
            elif "Payment" in name:
                domain_map[name] = "payment-processing"
            elif "Account" in name:
                domain_map[name] = "account-servicing"
            elif "Dealer" in name or "DealPackage" in name:
                domain_map[name] = "dealer-integration"
            else:
                domain_map[name] = "shared"

    scores = []
    for s in services:
        service_name = s["class_name"]
        service_domain = domain_map.get(service_name, "unknown")

        # Count outbound dependencies
        outbound_edges = [
            e for e in dep_graph.get("edges", []) if e["source"] == service_name
        ]
        inbound_edges = [
            e for e in dep_graph.get("edges", []) if e["target"] == service_name
        ]

        # Count cross-domain dependencies
        cross_domain_out = sum(
            1 for e in outbound_edges
            if domain_map.get(e["target"], "unknown") != service_domain
        )
        cross_domain_in = sum(
            1 for e in inbound_edges
            if domain_map.get(e["source"], "unknown") != service_domain
        )

        # Coupling score: weighted sum of cross-domain dependencies
        afferent = cross_domain_in  # incoming from other domains
        efferent = cross_domain_out  # outgoing to other domains
        total_coupling = afferent + efferent

        # Instability metric: Ce / (Ca + Ce)
        instability = (
            efferent / (afferent + efferent) if (afferent + efferent) > 0 else 0.0
        )

        # Risk score: combination of LOC, coupling, and method count
        loc_factor = min(s["line_count"] / 100, 5.0)
        coupling_factor = total_coupling * 2
        method_factor = s["method_count"] * 0.5
        risk_score = round(loc_factor + coupling_factor + method_factor, 1)

        scores.append({
            "service": service_name,
            "domain": service_domain,
            "afferent_coupling": afferent,
            "efferent_coupling": efferent,
            "total_coupling": total_coupling,
            "instability": round(instability, 2),
            "risk_score": risk_score,
            "outbound_dependencies": len(outbound_edges),
            "inbound_dependencies": len(inbound_edges),
            "cross_domain_calls": cross_domain_out,
            "line_count": s["line_count"],
            "method_count": s["method_count"],
            "autowired_count": s["autowired_dependencies"],
        })

    # Sort by risk score descending
    scores.sort(key=lambda x: x["risk_score"], reverse=True)
    return scores
