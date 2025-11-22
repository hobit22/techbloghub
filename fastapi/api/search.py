from fastapi import APIRouter

router = APIRouter(prefix="/api/search", tags=["search"])

@router.get("/posts")
async def search_posts(query: str):
    return {"message": "Hello World"}