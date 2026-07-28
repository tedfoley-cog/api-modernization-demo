"""Identify domain boundaries (bounded contexts) from service and endpoint analysis."""


DOMAIN_DEFINITIONS = {
    "loan-origination": {
        "description": "Loan application, credit decisioning, and funding workflow",
        "keywords": ["loan", "credit", "application", "approval", "funding", "origination"],
        "proposed_service": "loan-origination-service",
    },
    "payment-processing": {
        "description": "Payment submission, allocation, ACH processing, and late fee assessment",
        "keywords": ["payment", "ach", "fee", "batch", "allocation"],
        "proposed_service": "payment-processing-service",
    },
    "account-servicing": {
        "description": "Account management, payoff quotes, address updates, early termination",
        "keywords": ["account", "payoff", "balance", "termination", "address"],
        "proposed_service": "account-servicing-service",
    },
    "dealer-integration": {
        "description": "Dealer deal submission, settlement, reserve/holdback, inventory financing",
        "keywords": ["dealer", "deal", "reserve", "holdback", "settlement", "inventory"],
        "proposed_service": "dealer-integration-service",
    },
    "reporting": {
        "description": "Portfolio summary, delinquency reporting, dealer performance, regulatory",
        "keywords": ["report", "portfolio", "delinquency", "compliance", "tila", "ecoa"],
        "proposed_service": "reporting-service",
    },
}


def identify_domain_boundaries(
    endpoints: list[dict],
    services: list[dict],
    entities: list[dict],
    dep_graph: dict,
) -> list[dict]:
    """Map endpoints, services, and entities to proposed bounded contexts."""
    boundaries = []

    for domain_key, domain_def in DOMAIN_DEFINITIONS.items():
        keywords = domain_def["keywords"]

        # Map endpoints to this domain
        domain_endpoints = []
        for ep in endpoints:
            path_lower = ep["path"].lower()
            controller_lower = ep["controller"].lower()
            if any(kw in path_lower or kw in controller_lower for kw in keywords):
                domain_endpoints.append({
                    "method": ep["method"],
                    "path": ep["path"],
                    "controller": ep["controller"],
                })

        # Map services to this domain
        domain_services = []
        for s in services:
            name_lower = s["class_name"].lower()
            if any(kw in name_lower for kw in keywords):
                domain_services.append({
                    "class_name": s["class_name"],
                    "method_count": s["method_count"],
                    "line_count": s["line_count"],
                })

        # Map entities to this domain
        domain_entities = []
        for e in entities:
            name_lower = e["class_name"].lower()
            table_lower = e["table_name"].lower()
            if any(kw in name_lower or kw in table_lower for kw in keywords):
                domain_entities.append({
                    "class_name": e["class_name"],
                    "table_name": e["table_name"],
                })

        # Count cross-boundary dependencies for this domain
        domain_classes = {s["class_name"] for s in domain_services}
        cross_boundary = 0
        for edge in dep_graph.get("edges", []):
            if (edge["source"] in domain_classes) != (edge["target"] in domain_classes):
                cross_boundary += 1

        boundaries.append({
            "domain": domain_key,
            "description": domain_def["description"],
            "proposed_service": domain_def["proposed_service"],
            "endpoints": domain_endpoints,
            "endpoint_count": len(domain_endpoints),
            "services": domain_services,
            "entities": domain_entities,
            "cross_boundary_dependencies": cross_boundary,
            "extraction_complexity": _complexity_label(cross_boundary, domain_services),
        })

    return boundaries


def _complexity_label(cross_deps: int, services: list[dict]) -> str:
    """Estimate how hard it will be to extract this bounded context."""
    total_loc = sum(s.get("line_count", 0) for s in services)
    if cross_deps > 8 or total_loc > 400:
        return "HIGH"
    elif cross_deps > 4 or total_loc > 200:
        return "MEDIUM"
    else:
        return "LOW"
