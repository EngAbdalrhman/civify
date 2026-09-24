# Civify backend

This is the main Civify backend. In the shared workspace, read
`../Knowledge/README.md` and `../Knowledge/backend-contracts.md` before investigating
the MCP integration in `../civify-mcp/`.

Production is the deployed branch, confirmed on 2026-09-23. The current main checkout
is older and does not include the production external API controllers. Preserve the
checkout; inspect the appropriate ref with `git show origin/Production:<path>` for
contract verification. Check the ref has not advanced before relying on a recorded
contract. Record new verified findings in the shared Knowledge folder.
